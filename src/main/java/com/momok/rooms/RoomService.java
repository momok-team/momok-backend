package com.momok.rooms;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.momok.rooms.Dto.KakaoMapResponseDto;
import com.momok.rooms.Dto.NaverBlogResponseDto;
import com.momok.rooms.domain.RestaurantCard;
import com.momok.rooms.domain.VoteRoom;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
	private final RoomRepository roomRepository;

	private final RateLimitService rateLimitService;

	private final CaffeineCacheManager caffeineCacheManager;

	private final RestTemplate restTemplate;

	private Cache caffeineCacheData;

	private final String CAFFEINE_RESTAURANT_CARD_KEY = "caffeine_restaurant_key";

	@Value("${kakao.api.key}")
	private String KAKAO_API_KEY;

	@Value("${naver.client.id}")
	private String NAVER_CLIENT_ID;

	@Value("${naver.client.secret}")
	private String NAVER_CLIENT_SECRET;

	@PostConstruct
	public void initCaches() {
		this.caffeineCacheData = caffeineCacheManager.getCache("caffeine");
	}

	public VoteRoom addVoteRoom(double latitude, double longitude, Integer password) throws InterruptedException {
		if (latitude > 90 || latitude < -90) {
			throw new IllegalArgumentException("latitude는 90보다 작거나, -90보다 커야 합니다.");
		}

		if (longitude > 180 || longitude < -180) {
			throw new IllegalArgumentException("longitude는 180보다 작거나, -180보다 커야 합니다.");
		}

		List<RestaurantCard> restaurantCards = getRestaurantsFromKakaoMap(latitude, longitude);

		restaurantCards = getRestaurantsBlogReviewFromNaver(restaurantCards);

		VoteRoom voteRoom = roomRepository.save(VoteRoom.builder()
			.voteDeadline(LocalDateTime.now().plusMinutes(30))
			.latitude(latitude)
			.longitude(longitude)
			.password(password)
			.restaurantCards(restaurantCards)
			.build());

		caffeineCacheData.put(CAFFEINE_RESTAURANT_CARD_KEY + voteRoom.getId(), restaurantCards);

		return voteRoom;
	}

	private List<RestaurantCard> getRestaurantsFromKakaoMap(double latitude, double longitude) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Authorization", "KakaoAK " + KAKAO_API_KEY);

		HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
		List<RestaurantCard> restaurantCardList = new ArrayList<>();

		for (int page = 1; page <= 3; page++) {
			UriComponents uri = UriComponentsBuilder.fromUriString(
					"https://dapi.kakao.com/v2/local/search/category.json")
				.queryParam("category_group_code", "FD6")
				.queryParam("x", longitude)
				.queryParam("y", latitude)
				.queryParam("radius", "500")
				.queryParam("sort", "distance")
				.queryParam("page", page)
				.queryParam("size", "15")
				.encode()
				.build();

			KakaoMapResponseDto contents = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity,
				KakaoMapResponseDto.class).getBody();

			if (contents != null) {
				restaurantCardList.addAll(contents.getDocuments());
			}
		}

		return restaurantCardList.stream().filter(
			restaurantCard -> {
				String category = restaurantCard.getCategoryName();
				return category != null && !category.contains("술집") && !category.contains("간식");
			}).limit(25).toList();
	}

	private void awaitNaverQuota() throws InterruptedException {
		while (!rateLimitService.allowRequest("naver-api", 9)) {
			long sleepMillis = 1000 - (System.currentTimeMillis() % 1000) + 10;
			Thread.sleep(sleepMillis);
		}
	}

	private List<RestaurantCard> getRestaurantsBlogReviewFromNaver(List<RestaurantCard> restaurantCards) throws
		InterruptedException {

		for (RestaurantCard restaurantCard : restaurantCards) {
			awaitNaverQuota();

			log.info("restaurantCard.getName() = {}, restaurantCard.getAddressName() = {}", restaurantCard.getName(),
				restaurantCard.getAddressName());
			UriComponents uri = UriComponentsBuilder.fromUriString("https://openapi.naver.com/v1/search/blog.json")
				.queryParam("query", restaurantCard.getName() + " + " + restaurantCard.getAddressName())
				.queryParam("sort", "sim")
				.queryParam("display", "3")
				.queryParam("start", "1")
				.build();

			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.set("X-Naver-Client-Id", NAVER_CLIENT_ID);
			httpHeaders.set("X-Naver-Client-Secret", NAVER_CLIENT_SECRET);

			HttpEntity<String> entity = new HttpEntity<>(httpHeaders);

			ResponseEntity<NaverBlogResponseDto> response = restTemplate.exchange(uri.toString(), HttpMethod.GET,
				entity, NaverBlogResponseDto.class);

			if (response.getBody() != null) {
				restaurantCard.setReviews(response.getBody().getItems());
				restaurantCard.setTotalReview(response.getBody().getTotal());
			}
		}

		return restaurantCards;
	}

	public VoteRoom inquiryVoteRoom(String roomId) throws InterruptedException {
		VoteRoom voteRoom = roomRepository.findById(roomId).orElseThrow();
		List<RestaurantCard> cachedRestaurantCards = caffeineCacheData.get(CAFFEINE_RESTAURANT_CARD_KEY + roomId,
			List.class);
		if (cachedRestaurantCards != null) {
			voteRoom.setRestaurantCards(cachedRestaurantCards);
		} else {
			List<RestaurantCard> restaurantCards = getRestaurantsFromKakaoMap(voteRoom.getLatitude(),
				voteRoom.getLongitude());
			restaurantCards = getRestaurantsBlogReviewFromNaver(restaurantCards);
			voteRoom.setRestaurantCards(restaurantCards);
			caffeineCacheData.put(CAFFEINE_RESTAURANT_CARD_KEY + roomId, restaurantCards);
		}

		return voteRoom;
	}
}

package com.momok.rooms;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momok.global.JwtProvider;
import com.momok.rooms.Dto.GuestEnterRequestDto;
import com.momok.rooms.Dto.KakaoMapResponseDto;
import com.momok.rooms.Dto.NaverBlogResponseDto;
import com.momok.rooms.Dto.VoteResultResponseDto;
import com.momok.rooms.Dto.VoteSubmitRequestDto;
import com.momok.rooms.domain.RestaurantCard;
import com.momok.rooms.domain.VoteRoom;

import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
	private final RoomRepository roomRepository;

	private final RateLimitService rateLimitService;

	private final RedisCacheManager redisCacheManager;

	private final RestTemplate restTemplate;

	private Cache restaurantCardsCache;

	private Cache guestCache;

	private final String RESTAURANT_CARDS_CACHE_NAME = "restaurant_cards";

	private final String GUEST_DEVICE_CACHE_NAME = "guests";

	@Value("${kakao.api.key}")
	private String KAKAO_API_KEY;

	@Value("${naver.client.id}")
	private String NAVER_CLIENT_ID;

	@Value("${naver.client.secret}")
	private String NAVER_CLIENT_SECRET;

	private final JwtProvider jwtProvider;

	private final StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper;

	@PostConstruct
	public void initCaches() {
		this.restaurantCardsCache = Objects.requireNonNull(redisCacheManager.getCache(RESTAURANT_CARDS_CACHE_NAME),
			"restaurantCards cache must be configured");
		this.guestCache = Objects.requireNonNull(redisCacheManager.getCache(GUEST_DEVICE_CACHE_NAME),
			"guest_device cache must be configured");
	}

	public VoteRoom addVoteRoom(double latitude, double longitude, Integer password) throws InterruptedException {
		if (latitude > 90 || latitude < -90) {
			throw new IllegalArgumentException("latitude는 90보다 작거나, -90보다 커야 합니다.");
		}

		if (longitude > 180 || longitude < -180) {
			throw new IllegalArgumentException("longitude는 180보다 작거나, -180보다 커야 합니다.");
		}

		List<RestaurantCard> restaurantCards = getRestaurantsFromKakaoMap(latitude, longitude);

		getRestaurantsBlogReviewFromNaver(restaurantCards);
		getRestaurantThumbnailUrl(restaurantCards);

		VoteRoom voteRoom = roomRepository.save(VoteRoom.builder()
			.voteDeadline(LocalDateTime.now().plusMinutes(30))
			.latitude(latitude)
			.longitude(longitude)
			.password(password)
			.restaurantCards(restaurantCards)
			.build());

		restaurantCardsCache.put(voteRoom.getId(), restaurantCards);

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

	private void getRestaurantsBlogReviewFromNaver(List<RestaurantCard> restaurantCards) throws
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
				restaurantCard.setReviewCount(response.getBody().getTotal());
			}
		}
	}

	private void getRestaurantThumbnailUrl(List<RestaurantCard> restaurantCards) {
		for (RestaurantCard restaurantCard : restaurantCards) {
			HttpHeaders httpHeaders = new HttpHeaders();
			HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
			UriComponents uri = UriComponentsBuilder.fromUriString(
					"http://place.map.kakao.com/" + restaurantCard.getId())
				.encode()
				.build();

			try {
				String contents = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, String.class).getBody();

				if (contents != null) {
					restaurantCard.setThumbnailUrl(getOgImage(contents));
				}
			} catch (RestClientException re) {
				log.warn("thumbnail 가져오기 실패. restaurantCardId={}", restaurantCard.getId());
			}
		}
	}

	private String getOgImage(String html) {
		String regex = "<meta[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']";
		String regexSafe = "<meta[^>]*content=[\"']([^\"']+)[\"'][^>]*property=[\"']og:image[\"']";

		String imageUrl = null;

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(html);

		if (matcher.find()) {
			imageUrl = matcher.group(1);
		} else {
			pattern = Pattern.compile(regexSafe);
			matcher = pattern.matcher(html);
			if (matcher.find()) {
				imageUrl = matcher.group(1);
			}
		}

		if (imageUrl != null) {
			if (imageUrl.startsWith("//")) {
				imageUrl = "https:" + imageUrl;
			}
		}

		return imageUrl;
	}

	public VoteRoom inquiryVoteRoom(String roomId) throws InterruptedException {
		VoteRoom voteRoom = roomRepository.findById(roomId).orElseThrow();
		Object cachedValue = restaurantCardsCache.get(roomId, Object.class);

		if (cachedValue != null) {
			List<RestaurantCard> cachedRestaurantCards = objectMapper.convertValue(
				cachedValue,
				new TypeReference<List<RestaurantCard>>() {
				}
			);

			voteRoom.setRestaurantCards(cachedRestaurantCards);
		} else {
			List<RestaurantCard> restaurantCards = getRestaurantsFromKakaoMap(
				voteRoom.getLatitude(),
				voteRoom.getLongitude()
			);

			getRestaurantsBlogReviewFromNaver(restaurantCards);
			getRestaurantThumbnailUrl(restaurantCards);

			voteRoom.setRestaurantCards(restaurantCards);
			restaurantCardsCache.put(roomId, restaurantCards);
		}

		return voteRoom;
	}

	public String addGuest(String roomId, GuestEnterRequestDto guestEnterRequestDto) {
		VoteRoom voteRoom = roomRepository.findById(roomId).orElseThrow();
		if (voteRoom.getVoteDeadline().isBefore(LocalDateTime.now())) {
			throw new IllegalStateException("마감된 투표방입니다.");
		}

		// UUID 생성
		String uuid = UUID.randomUUID().toString();
		String deviceId = guestEnterRequestDto.getDeviceId();
		if (deviceId == null || deviceId.isBlank()) {
			throw new IllegalArgumentException("deviceId 값이 비어있습니다.");
		}

		String key = "roomId:" + roomId + ":deviceId:" + guestEnterRequestDto.getDeviceId();

		if (guestCache.get(key) == null) {
			log.info("GuestRandomUUID={}", uuid);
			guestCache.putIfAbsent(key, uuid);
		} else {
			uuid = guestCache.get(key, String.class);
		}

		HashMap<String, Object> claim = new HashMap<>();
		claim.put("roomId", roomId);
		claim.put("deviceId", guestEnterRequestDto.getDeviceId());
		return jwtProvider.generateAccessToken(uuid, claim);
	}

	public String saveForm(String roomId, String token, VoteSubmitRequestDto voteSubmitRequestDto) {
		if (!jwtProvider.validateToken(token)) {
			throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
		}

		String tokenRoomId = jwtProvider.getClaimFromToken(token, claims -> claims.get("roomId", String.class));

		if (!roomId.equals(tokenRoomId)) {
			throw new IllegalArgumentException("해당 방에 대한 투표 권한이 없습니다.");
		}

		String guestId = jwtProvider.getClaimFromToken(token, Claims::getSubject);

		VoteRoom voteRoom = roomRepository.findById(roomId).orElseThrow();

		if (voteRoom.getVoteDeadline().isBefore(LocalDateTime.now())) {
			throw new IllegalStateException("마감된 투표방입니다.");
		}

		if (voteSubmitRequestDto.getPlaceIds() == null || voteSubmitRequestDto.getPlaceIds().isEmpty()) {
			throw new IllegalArgumentException("투표할 음식점이 없습니다.");
		}

		Object cachedValue = restaurantCardsCache.get(roomId, Object.class);

		if (cachedValue == null) {
			throw new IllegalStateException("음식점 후보 목록을 찾을 수 없습니다.");
		}

		List<RestaurantCard> restaurantCards = objectMapper.convertValue(
			cachedValue,
			new TypeReference<List<RestaurantCard>>() {
			}
		);

		Set<Long> submittedPlaceIds = new HashSet<>(voteSubmitRequestDto.getPlaceIds());

		Set<Long> validPlaceIds = restaurantCards.stream()
			.map(RestaurantCard::getId)
			.collect(Collectors.toSet());

		for (Long placeId : submittedPlaceIds) {
			if (!validPlaceIds.contains(placeId)) {
				throw new IllegalArgumentException("투표방에 없는 음식점입니다. placeId=" + placeId);
			}
		}

		String votedKey = "voted:" + roomId + ":" + guestId;

		Duration ttl = Duration.between(LocalDateTime.now(), voteRoom.getVoteDeadline());

		Boolean firstVote = stringRedisTemplate.opsForValue().setIfAbsent(votedKey, "1", ttl);

		if (Boolean.FALSE.equals(firstVote)) {
			throw new IllegalStateException("이미 투표한 게스트입니다.");
		}

		String voteCountKey = "vote-count:" + roomId;
		for (Long placeId : submittedPlaceIds) {
			stringRedisTemplate.opsForHash()
				.increment(voteCountKey, String.valueOf(placeId), 1);
		}

		return "success";
	}

	public VoteResultResponseDto getResults(String roomId) {
		VoteRoom voteRoom = roomRepository.findById(roomId).orElseThrow();

		Object cachedValue = restaurantCardsCache.get(roomId, Object.class);

		List<RestaurantCard> restaurantCards = objectMapper.convertValue(
			cachedValue,
			new TypeReference<List<RestaurantCard>>() {
			}
		);

		if (restaurantCards == null) {
			throw new IllegalStateException("음식점 후보 목록을 찾을 수 없습니다.");
		}

		String voteCountKey = "vote-count:" + roomId;
		Map<Object, Object> voteCounts = stringRedisTemplate.opsForHash()
			.entries(voteCountKey);

		List<VoteResultResponseDto.VoteResultItem> results = restaurantCards.stream()
			.map(restaurantCard -> {
				Object countValue = voteCounts.get(String.valueOf(restaurantCard.getId()));
				Long voteCount = countValue == null ? 0L : Long.parseLong(String.valueOf(countValue));

				return new VoteResultResponseDto.VoteResultItem(
					restaurantCard,
					voteCount,
					0
				);
			})
			.sorted(
				Comparator
					.comparing(VoteResultResponseDto.VoteResultItem::getVoteCount)
					.reversed()
			)
			.limit(3)
			.toList();

		List<VoteResultResponseDto.VoteResultItem> rankedResults = new ArrayList<>();

		for (int i = 0; i < results.size(); i++) {
			VoteResultResponseDto.VoteResultItem item = results.get(i);

			rankedResults.add(new VoteResultResponseDto.VoteResultItem(
				item.getRestaurantCard(),
				item.getVoteCount(),
				i + 1
			));
		}

		return VoteResultResponseDto.builder()
			.roomId(voteRoom.getId())
			.results(rankedResults)
			.build();
	}
}

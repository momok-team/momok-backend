package com.momok.rooms;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momok.global.config.SecurityConfig;
import com.momok.rooms.Dto.VoteRoomDetailsResponseDto;
import com.momok.rooms.Dto.VoteRoomRequestDto;
import com.momok.rooms.domain.RestaurantCard;
import com.momok.rooms.domain.VoteRoom;

@WebMvcTest(RoomController.class)
@Import(SecurityConfig.class)
class RoomControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	RoomService roomService;

	@Test
	@DisplayName("방 생성 성공 테스트")
	void 방_생성_성공() throws Exception {
		// given
		double latitude = 37.12345;
		double longitude = 126.12345;
		Integer password = 1234;
		VoteRoomRequestDto voteRoomRequestDto = new VoteRoomRequestDto(password,
			new VoteRoomRequestDto.Location(latitude, longitude));
		String id = "aaaaaaaa-bbbb-cccc-ddddeeeeffff";
		List<RestaurantCard> restaurantCards = new ArrayList<>();
		// RestaurantCard restaurantCard = new RestaurantCard();
		// restaurantCards.add(restaurantCard);
		LocalDateTime timeNow = LocalDateTime.now().plusMinutes(30);
		VoteRoom voteRoom = VoteRoom.builder()
			.id(id)
			.latitude(latitude)
			.longitude(longitude)
			.password(password)
			.voteDeadline(timeNow)
			.restaurantCards(restaurantCards)
			.build();

		given(roomService.addVoteRoom(latitude, longitude, password)).willReturn(voteRoom);

		// when & then
		MvcResult result = mockMvc.perform(post("/rooms").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(voteRoomRequestDto)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.location.latitude").value(voteRoomRequestDto.getLocation().getLatitude()))
			.andExpect(jsonPath("$.location.longitude").value(voteRoomRequestDto.getLocation().getLongitude()))
			.andExpect(jsonPath("$.password").value(password))
			.andExpect(jsonPath("$.restaurantCards").value(restaurantCards))
			.andExpect(jsonPath("$.roomId").value(id))
			.andReturn();

		VoteRoomDetailsResponseDto response = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			VoteRoomDetailsResponseDto.class
		);

		assertThat(response.getVoteDeadline()).isEqualTo(timeNow);
	}
}

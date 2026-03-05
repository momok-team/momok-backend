package com.momok.rooms;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momok.global.config.SecurityConfig;
import com.momok.rooms.Dto.VoteRoomRequestDto;

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
		VoteRoom voteRoom = VoteRoom.builder()
			.id(id)
			.latitude(latitude)
			.longitude(longitude)
			.password(password)
			.voteDeadline(LocalDateTime.now().plusMinutes(30))
			.build();

		given(roomService.saveVoteRoom(latitude, longitude, password)).willReturn(voteRoom);

		// when & then
		mockMvc.perform(post("/rooms").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(voteRoomRequestDto)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.roomUrl").value("https://momok.site/rooms/"+ id));
	}
}

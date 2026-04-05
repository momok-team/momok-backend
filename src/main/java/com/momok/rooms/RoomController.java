package com.momok.rooms;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.momok.rooms.Dto.GuestEnterRequestDto;
import com.momok.rooms.Dto.GuestEnterResponseDto;
import com.momok.rooms.Dto.VoteRoomDetailsResponseDto;
import com.momok.rooms.Dto.VoteRoomRequestDto;
import com.momok.rooms.Dto.VoteRoomResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
@Tag(name = "VoteRoom", description = "투표 방 관련 API")
public class RoomController {
	private final RoomService roomService;

	@PostMapping
	@Operation(summary = "방 생성", description = "새로운 음식점 투표 방을 생성합니다.")
	@ApiResponse(responseCode = "201", description = "방이 성공적으로 생성되었습니다.")
	public ResponseEntity<VoteRoomDetailsResponseDto> createVoteRoom(
		@RequestBody VoteRoomRequestDto voteRoomRequestDto) throws
		InterruptedException {
		if (voteRoomRequestDto == null || voteRoomRequestDto.getLocation() == null) {
			return ResponseEntity.badRequest().build();
		}

		VoteRoomRequestDto.Location location = voteRoomRequestDto.getLocation();

		if (location.getLatitude() == null || location.getLongitude() == null) {
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(roomService.addVoteRoom(location.getLatitude(), location.getLongitude(),
				voteRoomRequestDto.getPassword()).toDetailDto());
	}

	@GetMapping("/{roomId}")
	@Operation(summary = "방 조회", description = "투표 방을 조회합니다.")
	@ApiResponse(responseCode = "200")
	public ResponseEntity<VoteRoomResponseDto> getVoteRoom(@PathVariable String roomId) throws
		InterruptedException {
		return ResponseEntity.status(HttpStatus.OK).body(roomService.inquiryVoteRoom(roomId).toDto());
	}

	@PostMapping("/{roomId}/enter")
	public ResponseEntity<GuestEnterResponseDto> enterGuest(@PathVariable String roomId,
		@RequestBody GuestEnterRequestDto guestEnterRequestDto) {
		return ResponseEntity.status(HttpStatus.OK)
			.body(new GuestEnterResponseDto(roomService.addGuest(roomId, guestEnterRequestDto)));
	}
}

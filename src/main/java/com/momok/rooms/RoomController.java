package com.momok.rooms;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.momok.rooms.Dto.VoteRoomRequestDto;
import com.momok.rooms.Dto.VoteRoomResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class RoomController {
	private final RoomService roomService;

	@PostMapping
	public ResponseEntity<VoteRoomResponseDto> createVoteRoom(@RequestBody VoteRoomRequestDto voteRoomRequestDto) {
		if (voteRoomRequestDto == null || voteRoomRequestDto.getLocation() == null) {
			return ResponseEntity.badRequest().build();
		}

		VoteRoomRequestDto.Location location = voteRoomRequestDto.getLocation();

		if (location.getLatitude() == null || location.getLongitude() == null) {
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(roomService.saveVoteRoom(location.getLatitude(), location.getLongitude(),
				voteRoomRequestDto.getPassword()).toDto());
	}
}

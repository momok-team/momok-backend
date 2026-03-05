package com.momok.rooms;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {
	private final RoomRepository roomRepository;

	public VoteRoom saveVoteRoom(double latitude, double longitude, Integer password) {
		return roomRepository.save(VoteRoom.builder()
			.voteDeadline(LocalDateTime.now().plusMinutes(30))
			.latitude(latitude)
			.longitude(longitude)
			.password(password)
			.build()
		);
	}
}

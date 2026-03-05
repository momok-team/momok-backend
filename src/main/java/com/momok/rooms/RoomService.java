package com.momok.rooms;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {
	private final RoomRepository roomRepository;

	public VoteRoom saveVoteRoom(double latitude, double longitude, Integer password) {
		if (latitude > 90 || latitude < -90) {
			throw new IllegalArgumentException("latitude는 90보다 작거나, -90보다 커야 합니다.");
		}

		if (longitude > 180 || longitude < -180) {
			throw new IllegalArgumentException("longitude는 180보다 작거나, -180보다 커야 합니다.");
		}
		return roomRepository.save(VoteRoom.builder()
			.voteDeadline(LocalDateTime.now().plusMinutes(30))
			.latitude(latitude)
			.longitude(longitude)
			.password(password)
			.build()
		);
	}
}

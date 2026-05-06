package com.momok.rooms.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresenceCountResponseDto {

	private String roomId;
	private long activeUserCount;
}

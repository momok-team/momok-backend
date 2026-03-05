package com.momok.rooms.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VoteRoomRequestDto {
	private Integer password;

	private Location location;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Location {
		private double latitude;
		private double longitude;
	}
}

package com.momok.rooms.Dto;

import java.time.LocalDateTime;
import java.util.List;

import com.momok.rooms.domain.RestaurantCard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class VoteRoomResponseDto {
	private String roomId;

	private LocalDateTime voteDeadline;

	private VoteRoomDetailsResponseDto.Location location;

	private List<RestaurantCard> restaurantCards;

	@Getter
	@AllArgsConstructor
	public static class Location {
		private Double latitude;

		private Double longitude;
	}
}

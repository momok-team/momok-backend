package com.momok.rooms.Dto;

import java.util.List;

import com.momok.rooms.domain.RestaurantCard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VoteResultResponseDto {
	private String roomId;
	private List<VoteResultItem> results;

	@Getter
	@AllArgsConstructor
	public static class VoteResultItem {
		private RestaurantCard restaurantCard;
		private Long voteCount;
		private Integer rank;
	}
}

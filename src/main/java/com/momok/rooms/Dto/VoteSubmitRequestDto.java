package com.momok.rooms.Dto;

import java.util.List;

import lombok.Getter;

@Getter
public class VoteSubmitRequestDto {
	private List<Long> placeIds;
}

package com.momok.rooms.Dto;

import java.util.List;

import com.momok.rooms.domain.RestaurantCard;

import lombok.Getter;

@Getter
public class KakaoMapResponseDto {
	private List<RestaurantCard> documents;
}

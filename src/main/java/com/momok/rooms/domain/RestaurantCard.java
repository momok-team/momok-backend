package com.momok.rooms.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.momok.rooms.Dto.NaverBlogResponseDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantCard {
	private Long id;

	@JsonProperty("place_name")
	private String name;

	@JsonProperty("category_name")
	private String categoryName;

	@JsonProperty("address_name")
	private String addressName;

	private String distance;

	@JsonProperty("y")
	private String latitude;

	@JsonProperty("x")
	private String longitude;

	@JsonProperty("place_url")
	private String placeUrl;

	private List<NaverBlogResponseDto.BlogItem> reviews;

	private Integer totalReview;
}

package com.momok.rooms.Dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class NaverBlogResponseDto {
	private List<BlogItem> items;
	private Integer total;

	@Getter
	public static class BlogItem {
		private String title;

		private String description;

		@JsonProperty("bloggername")
		private String bloggerName;

		@JsonProperty("postdate")
		private String postDate;
	}
}

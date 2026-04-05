package com.momok.rooms.Dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GuestEnterResponseDto {
	private final String sessionToken;
}

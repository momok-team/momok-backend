package com.momok.rooms;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.momok.rooms.Dto.PresencePingRequestDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PresenceWebSocketController {

	private final PresenceService presenceService;

	@MessageMapping("/rooms/{roomId}/ping")
	public void ping(
		@DestinationVariable String roomId,
		PresencePingRequestDto request
	) {
		presenceService.ping(roomId, request.getUserId());
	}
}

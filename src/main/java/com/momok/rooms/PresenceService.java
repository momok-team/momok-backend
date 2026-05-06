package com.momok.rooms;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.momok.global.JwtProvider;
import com.momok.rooms.Dto.PresenceCountResponseDto;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService {

	private static final long ACTIVE_WINDOW_SECONDS = 20L;

	private final StringRedisTemplate stringRedisTemplate;

	private final SimpMessagingTemplate messagingTemplate;

	private final JwtProvider jwtProvider;

	public long ping(String roomId, String sessionToken) {
		if (!jwtProvider.validateToken(sessionToken)) {
			throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
		}

		String tokenRoomId = jwtProvider.getClaimFromToken(
			sessionToken,
			claims -> claims.get("roomId", String.class)
		);

		if (!roomId.equals(tokenRoomId)) {
			throw new IllegalArgumentException("해당 방에 대한 접근 권한이 없습니다.");
		}

		String guestId = jwtProvider.getClaimFromToken(sessionToken, Claims::getSubject);

		long now = Instant.now().getEpochSecond();
		String key = presenceKey(roomId);

		stringRedisTemplate.opsForZSet().add(key, guestId, now);
		stringRedisTemplate.expire(key, ACTIVE_WINDOW_SECONDS * 3, TimeUnit.SECONDS);

		long activeCount = cleanupAndCount(roomId, now);
		publishCount(roomId, activeCount);

		return activeCount;
	}

	public long getActiveUserCount(String roomId) {
		return cleanupAndCount(roomId, Instant.now().getEpochSecond());
	}

	@Scheduled(fixedDelay = 10000)
	public void cleanupAllRooms() {
		Set<String> keys = stringRedisTemplate.keys("presence:room:*");
		if (keys == null || keys.isEmpty()) {
			return;
		}

		long now = Instant.now().getEpochSecond();
		for (String key : keys) {
			String roomId = key.substring("presence:room:".length());
			long activeCount = cleanupAndCount(roomId, now);
			publishCount(roomId, activeCount);
		}
	}

	private long cleanupAndCount(String roomId, long nowEpochSecond) {
		String key = presenceKey(roomId);
		long expiredBefore = nowEpochSecond - ACTIVE_WINDOW_SECONDS;

		stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, expiredBefore);

		Long count = stringRedisTemplate.opsForZSet().zCard(key);
		return count == null ? 0L : count;
	}

	private void publishCount(String roomId, long activeCount) {
		messagingTemplate.convertAndSend(
			"/topic/rooms/" + roomId + "/presence",
			new PresenceCountResponseDto(roomId, activeCount)
		);
	}

	private String presenceKey(String roomId) {
		return "presence:room:" + roomId;
	}
}

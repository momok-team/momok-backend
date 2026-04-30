package com.momok.rooms;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.momok.rooms.Dto.PresenceCountResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService {

	private static final long ACTIVE_WINDOW_SECONDS = 20L;

	private final StringRedisTemplate stringRedisTemplate;

	private final SimpMessagingTemplate messagingTemplate;

	public long ping(String roomId, String guestId) {
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

package com.momok.rooms;

import java.time.Duration;
import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateLimitService {

	private final StringRedisTemplate stringRedisTemplate;

	public boolean allowRequest(String apiName, long limitPerSecond) {
		long nowSecond = Instant.now().getEpochSecond();
		String key = "rate:" + apiName + ":" + nowSecond;

		Long count = stringRedisTemplate.opsForValue().increment(key);

		if (count != null && count == 1L) {
			stringRedisTemplate.expire(key, Duration.ofSeconds(2));
		}

		return count != null && count <= limitPerSecond;
	}
}

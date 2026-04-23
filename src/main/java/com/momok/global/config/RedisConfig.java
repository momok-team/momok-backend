package com.momok.global.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {
	@Value("${spring.data.redis.host}")
	private String redisHost;

	@Value("${spring.data.redis.port}")
	private int redisPort;

	@Value("${spring.data.redis.username}")
	private String redisUsername;

	@Value("${spring.data.redis.password}")
	private String redisPassword;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
		configuration.setHostName(redisHost);
		configuration.setPort(redisPort);
		configuration.setUsername(redisUsername);
		configuration.setPassword(redisPassword);

		return new LettuceConnectionFactory(configuration);
	}

	@Bean
	public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		var serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

		// 캐시 항목 기본 설정 (TTL 설정 등)
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
			// 캐시 값의 직렬화 방식을 JSON으로 설정 (객체 저장을 위해)
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
			// 캐시 만료 시간 설정 (예: 1시간)
			.entryTtl(Duration.ofHours(24));

		RedisCacheConfiguration restaurantCardsConfig = RedisCacheConfiguration.defaultCacheConfig()
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
			.entryTtl(Duration.ofHours(24));

		return RedisCacheManager.RedisCacheManagerBuilder
			.fromConnectionFactory(connectionFactory)
			.cacheDefaults(config)
			.withCacheConfiguration("guests", config)
			.withCacheConfiguration("restaurant_cards", restaurantCardsConfig)
			.build();
	}
}

package com.momok.global.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CaffeineConfig {
	@Bean
	public CaffeineCacheManager caffeineCacheManager() {
		CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
		caffeineCacheManager.setCaffeine(
			Caffeine.newBuilder().expireAfterWrite(31, TimeUnit.MINUTES).maximumSize(100000));
		return caffeineCacheManager;
	}
}

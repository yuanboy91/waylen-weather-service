package com.waylen.weather.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * Caffeine-backed cache configuration, one independent region per concern.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@EnableCaching
@Configuration
public class CacheConfig {

    /** Cache used by WeatherService — short TTL, weather changes fast. */
    public static final String WEATHER_CACHE = "weather";

    @Bean
    public CacheManager cacheManager(WeatherCacheProperties props) {
        log.info("Configuring '{}' cache: ttl={}, maxSize={}",
                WEATHER_CACHE, props.getTtl(), props.getMaximumSize());

        CaffeineCache weatherCache = new CaffeineCache(WEATHER_CACHE, Caffeine.newBuilder()
                .expireAfterWrite(props.getTtl())
                .maximumSize(props.getMaximumSize())
                .recordStats()
                .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Collections.singletonList(weatherCache));
        return manager;
    }

}
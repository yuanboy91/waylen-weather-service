package com.waylen.weather.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine-backed cache configuration for the weather service layer.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@EnableCaching
@Configuration
public class CacheConfig {

    /**
     * Name of the cache used by {@link com.waylen.weather.service.WeatherService}.
     */
    public static final String WEATHER_CACHE = "weather";

    @Bean
    public CacheManager cacheManager(WeatherCacheProperties props) {
        log.info("Configuring '{}' cache: ttl={}, maxSize={}",
                WEATHER_CACHE, props.getTtl(), props.getMaximumSize());

        CaffeineCacheManager manager = new CaffeineCacheManager(WEATHER_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(props.getTtl())
                .maximumSize(props.getMaximumSize())
                .recordStats());
        return manager;
    }

}

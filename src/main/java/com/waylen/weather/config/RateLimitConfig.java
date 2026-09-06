package com.waylen.weather.config;

import com.google.common.util.concurrent.RateLimiter;
import com.waylen.weather.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the process-local {@link RateLimiter} used by
 * {@link RateLimitInterceptor} on the weather API endpoints.
 *
 * @author Waylen
 * @date 2026/9/6
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter weatherRateLimiter(@Value("${app.rate-limit.per-second:1}") double permitsPerSecond) {
        return RateLimiter.create(permitsPerSecond);
    }

}

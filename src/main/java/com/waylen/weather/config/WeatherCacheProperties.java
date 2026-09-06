package com.waylen.weather.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Cache tunables bound to {@code openweathermap.cache.*}.
 *
 * @author Waylen
 * @date 2026/9/6
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "openweathermap.cache")
public class WeatherCacheProperties {

    /** Time after which a cached entry is evicted. Default: 10 minutes. */
    private Duration ttl = Duration.ofMinutes(10);

    /** Maximum number of entries; oldest are evicted beyond this. */
    private long maximumSize = 500L;

}

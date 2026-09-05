package com.waylen.weather.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
/**
 * Caffeine-backed cache configuration for the service layer.
 * <p>
 * The {@code weather} cache stores {@code WeatherResponse} instances keyed by
 * their query parameters (city, ZIP+country, lat+lon). It exists so the
 * OpenWeatherMap upstream is not hammered when several callers ask about the
 * same place in quick succession — current weather is not a real-time concern
 * for most uses, and the upstream has aggressive rate limits on free keys.
 * <p>
 * Tunables are exposed via {@link WeatherCacheProperties} so the TTL and size
 * can be adjusted per environment without rebuilding. Defaults give roughly a
 * 10-minute freshness window with up to 500 entries, which is enough for
 * typical traffic and bounded to keep memory predictable.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@EnableCaching
@Configuration
public class CacheConfig {

    /** Name of the cache used by {@link com.waylen.weather.service.WeatherService}. */
    public static final String WEATHER_CACHE = "weather";

    /**
     * Caffeine configuration bound to {@code openweathermap.cache.*} properties.
     */
    @Bean
    @ConfigurationProperties(prefix = "openweathermap.cache")
    public WeatherCacheProperties weatherCacheProperties() {
        return new WeatherCacheProperties();
    }

    /**
     * Build a {@link CacheManager} from the {@link WeatherCacheProperties} and
     * register the {@value #WEATHER_CACHE} cache (plus an alias map so any
     * future named cache gets the same defaults).
     */
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

    /**
     * Tunable cache settings. Bound to {@code openweathermap.cache.*} so they
     * can be adjusted per environment.
     *
     * <p>Default: 10-minute TTL, 500 entries — generous enough for typical
     * traffic but bounded to keep JVM heap usage predictable.</p>
     */
    public static class WeatherCacheProperties {

        /** Time after which a cached entry is forcibly refreshed. */
        private Duration ttl = Duration.ofMinutes(10);

        /** Maximum number of entries to keep; oldest are evicted beyond this. */
        private long maximumSize = 500L;

        public Duration getTtl() {
            return ttl;
        }

        public void setTtlSeconds(long seconds) {
            this.ttl = Duration.ofSeconds(seconds);
        }

        /** Framework-friendly setter (Duration in YAML). */
        public void setTtl(Duration ttl) {
            if (ttl != null) {
                this.ttl = ttl;
            }
        }

        public long getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
        }

        /** Convenience for callers that prefer raw seconds. */
        public long ttlSeconds() {
            return ttl.getSeconds();
        }
    }
}

package com.waylen.weather.service;

import com.waylen.weather.client.OpenWeatherClient;
import com.waylen.weather.config.CacheConfig;
import com.waylen.weather.model.domain.WeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Weather lookup service — delegates to {@link OpenWeatherClient}, which
 * returns the already-converted {@link WeatherResponse} domain model, and
 * caches the results.
 *
 * @author Waylen
 * @date 2026/9/6
 */
@Slf4j
@Service
public class WeatherService {

    @Autowired
    private OpenWeatherClient openWeatherClient;

    /**
     * Look up current weather by city name (e.g. "London" or "London,GB").
     */
    @Cacheable(value = CacheConfig.WEATHER_CACHE, key = "#city", unless = "#result == null")
    public WeatherResponse getCurrentWeatherByCity(String city) {
        log.info("Querying weather by city: {}", city);
        return openWeatherClient.getCurrentWeatherByCity(city);
    }

    /**
     * Look up current weather by ZIP/postal code + country.
     */
    @Cacheable(value = CacheConfig.WEATHER_CACHE,
            key = "#zip + ',' + #countryCode", unless = "#result == null")
    public WeatherResponse getCurrentWeatherByZip(String zip, String countryCode) {
        log.info("Querying weather by ZIP: {}/{}", zip, countryCode);
        return openWeatherClient.getCurrentWeatherByZip(zip, countryCode);
    }

    /**
     * Look up current weather by geographic coordinates.
     */
    @Cacheable(value = CacheConfig.WEATHER_CACHE,
            key = "#lat + ',' + #lon", unless = "#result == null")
    public WeatherResponse getCurrentWeatherByCoordinates(double lat, double lon) {
        log.info("Querying weather by coordinates: lat={}, lon={}", lat, lon);
        return openWeatherClient.getCurrentWeatherByCoordinates(lat, lon);
    }

    /**
     * Look up current weather by OpenWeatherMap city ID.
     */
    @Cacheable(value = CacheConfig.WEATHER_CACHE,
            key = "'id:' + #cityId", unless = "#result == null")
    public WeatherResponse getCurrentWeatherByCityId(String cityId) {
        log.info("Querying weather by city ID: {}", cityId);
        return openWeatherClient.getCurrentWeatherByCityId(cityId);
    }

}

package com.waylen.weather.service;

import com.alibaba.fastjson.JSONObject;
import com.waylen.weather.client.OpenWeatherClient;
import com.waylen.weather.config.CacheConfig;
import com.waylen.weather.model.domain.WeatherResponse;
import com.waylen.weather.model.domain.WeatherResponse.Wind;
import com.waylen.weather.model.dto.OpenWeatherDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Weather lookup service — delegates to {@link OpenWeatherClient},
 * converts the provider's JSON into {@link OpenWeatherDTO},
 * then maps it to {@link WeatherResponse}.
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
        return toWeather(openWeatherClient.getCurrentWeatherByCity(city));
    }

    /**
     * Look up current weather by ZIP/postal code + country.
     */
    @Cacheable(value = CacheConfig.WEATHER_CACHE,
            key = "#zip + ',' + #countryCode", unless = "#result == null")
    public WeatherResponse getCurrentWeatherByZip(String zip, String countryCode) {
        log.info("Querying weather by ZIP: {}/{}", zip, countryCode);
        return toWeather(openWeatherClient.getCurrentWeatherByZip(zip, countryCode));
    }

    /**
     * Look up current weather by geographic coordinates.
     */
    @Cacheable(value = CacheConfig.WEATHER_CACHE,
            key = "#lat + ',' + #lon", unless = "#result == null")
    public WeatherResponse getCurrentWeatherByCoordinates(double lat, double lon) {
        log.info("Querying weather by coordinates: lat={}, lon={}", lat, lon);
        return toWeather(openWeatherClient.getCurrentWeatherByCoordinates(lat, lon));
    }

    /**
     * Map provider JSON to domain model via DTO, null-safe on all sections.
     */
    private WeatherResponse toWeather(JSONObject json) {
        OpenWeatherDTO dto = json.toJavaObject(OpenWeatherDTO.class);

        OpenWeatherDTO.Wind windDto = dto.getWind();
        Wind wind = (windDto != null)
                ? Wind.builder().speed(windDto.getSpeed()).degree(windDto.getDeg()).gust(windDto.getGust()).build()
                : null;

        OpenWeatherDTO.Main main = dto.getMain();
        java.util.List<OpenWeatherDTO.Weather> conditions = dto.getWeather();
        OpenWeatherDTO.Weather weather = (conditions != null && !conditions.isEmpty()) ? conditions.get(0) : null;

        OpenWeatherDTO.Sys sys = dto.getSys();

        return WeatherResponse.builder()
                .locationName(dto.getName())
                .country(sys != null ? sys.getCountry() : null)
                .condition(weather != null ? weather.getMain() : null)
                .description(weather != null ? weather.getDescription() : null)
                .iconCode(weather != null ? weather.getIcon() : null)
                .temperature(main != null ? main.getTemp() : null)
                .feelsLike(main != null ? main.getFeelsLike() : null)
                .humidity(main != null ? main.getHumidity() : null)
                .pressure(main != null ? main.getPressure() : null)
                .wind(wind)
                .cloudiness(dto.getClouds() != null ? dto.getClouds().getAll() : null)
                .visibility(dto.getVisibility())
                .build();
    }

}
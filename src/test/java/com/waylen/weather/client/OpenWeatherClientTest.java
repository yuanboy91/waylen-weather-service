package com.waylen.weather.client;

import com.alibaba.fastjson.JSON;
import com.waylen.weather.exception.LocationNotFoundException;
import com.waylen.weather.model.dto.OpenWeatherDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link OpenWeatherClient}.
 * <p>
 * These tests hit the real OpenWeatherMap API and therefore require a
 * valid {@code OPENWEATHERMAP_API_KEY} environment variable. The class is
 * guarded with {@code @EnabledIfEnvironmentVariable} so running
 * {@code mvn test} without setting the key simply skips the whole class
 * instead of failing with HTTP 401. For local dev:
 * <pre>
 *   export OPENWEATHERMAP_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   mvn test
 * </pre>
 * Use {@code make test-offline} to run only the cache + controller tests
 * that do not need network access.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENWEATHERMAP_API_KEY", matches = ".+")
public class OpenWeatherClientTest {

    @Autowired
    private OpenWeatherClient openWeatherClient;

    // --------------------------------------------------------- Success cases

    @Nested
    @DisplayName("Successful queries")
    class SuccessTests {

        @Test
        @DisplayName("getCurrentWeatherByCity – Beijing")
        void byCity() {
            OpenWeatherDTO dto = openWeatherClient.getCurrentWeatherByCity("Beijing");
            log.info("byCity response: {}", JSON.toJSONString(dto));

            assertNotNull(dto, "DTO must not be null");
            assertNotNull(dto.getName(), "Location name must be populated");
            assertNotNull(dto.getMain(), "Main section must be populated");
            assertNotNull(dto.getMain().getTemp(), "Temperature must not be null");
            assertNotNull(dto.getCoord(), "Coordinates must be populated");
            assertNotNull(dto.getWeather(), "Weather list must not be null");
            assertFalse(dto.getWeather().isEmpty(), "Weather list must not be empty");
        }

        @Test
        @DisplayName("getCurrentWeatherByCity – with country code (London,GB)")
        void byCityWithCountry() {
            OpenWeatherDTO dto = openWeatherClient.getCurrentWeatherByCity("London,GB");
            log.info("byCityWithCountry response: {}", JSON.toJSONString(dto));

            assertNotNull(dto);
            assertNotNull(dto.getName());
            assertNotNull(dto.getSys());
            assertEquals("GB", dto.getSys().getCountry());
        }

        @Test
        @DisplayName("getCurrentWeatherByZip – 10001,US (New York)")
        void byZip() {
            OpenWeatherDTO dto = openWeatherClient.getCurrentWeatherByZip("10001", "US");
            log.info("byZip response: {}", JSON.toJSONString(dto));

            assertNotNull(dto);
            assertNotNull(dto.getName());
            assertNotNull(dto.getMain());
            assertNotNull(dto.getMain().getTemp());
        }

        @Test
        @DisplayName("getCurrentWeatherByCoordinates – Beijing (39.909, 116.397)")
        void byCoordinates() {
            OpenWeatherDTO dto = openWeatherClient.getCurrentWeatherByCoordinates(39.909, 116.397);
            log.info("byCoordinates response: {}", JSON.toJSONString(dto));

            assertNotNull(dto);
            assertNotNull(dto.getCoord());
            assertNotNull(dto.getCoord().getLat());
            assertNotNull(dto.getCoord().getLon());
            assertNotNull(dto.getMain());
        }
    }

    // --------------------------------------------------------- Error cases

    @Nested
    @DisplayName("Error / not-found queries")
    class ErrorTests {

        @Test
        @DisplayName("Invalid city name should throw LocationNotFoundException")
        void invalidCity() {
            assertThrows(LocationNotFoundException.class, () ->
                    openWeatherClient.getCurrentWeatherByCity("XxNoSuchCityxX_12345"));
        }

        @Test
        @DisplayName("Invalid ZIP code should throw LocationNotFoundException")
        void invalidZip() {
            assertThrows(LocationNotFoundException.class, () ->
                    openWeatherClient.getCurrentWeatherByZip("00000", "ZZ"));
        }
    }
}

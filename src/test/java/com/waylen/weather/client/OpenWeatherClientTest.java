package com.waylen.weather.client;

import com.waylen.weather.exception.LocationNotFoundException;
import com.waylen.weather.model.domain.WeatherResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link OpenWeatherClient} hitting the real
 * OpenWeatherMap API. The whole class is skipped unless the
 * {@code OPENWEATHERMAP_API_KEY} environment variable is set:
 * <pre>
 *   export OPENWEATHERMAP_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   mvn test
 * </pre>
 *
 * @author Waylen
 * @date 2026/9/5
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENWEATHERMAP_API_KEY", matches = ".+")
public class OpenWeatherClientTest {

    @Autowired
    private OpenWeatherClient openWeatherClient;

    @Test
    void byCity_shouldReturnWeather() {
        WeatherResponse weather = openWeatherClient.getCurrentWeatherByCity("Beijing");

        assertNotNull(weather.getLocationName());
        assertNotNull(weather.getTemperature());
        assertNotNull(weather.getCondition());
    }

    @Test
    void byCityWithCountry_shouldResolveCountryCode() {
        WeatherResponse weather = openWeatherClient.getCurrentWeatherByCity("London,GB");

        assertEquals("GB", weather.getCountry());
    }

    @Test
    void byZip_shouldReturnWeather() {
        WeatherResponse weather = openWeatherClient.getCurrentWeatherByZip("10001", "US");

        assertNotNull(weather.getLocationName());
        assertNotNull(weather.getTemperature());
    }

    @Test
    void byCoordinates_shouldReturnWeather() {
        WeatherResponse weather = openWeatherClient.getCurrentWeatherByCoordinates(39.909, 116.397);

        assertNotNull(weather.getLocationName());
        assertNotNull(weather.getTemperature());
    }

    @Test
    void byInvalidCity_shouldThrowLocationNotFound() {
        assertThrows(LocationNotFoundException.class,
                () -> openWeatherClient.getCurrentWeatherByCity("XxNoSuchCityxX_12345"));
    }

    @Test
    void byInvalidZip_shouldThrowLocationNotFound() {
        assertThrows(LocationNotFoundException.class,
                () -> openWeatherClient.getCurrentWeatherByZip("00000", "ZZ"));
    }

}

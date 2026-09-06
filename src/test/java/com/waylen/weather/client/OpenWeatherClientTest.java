package com.waylen.weather.client;

import com.alibaba.fastjson.JSONObject;
import com.waylen.weather.exception.LocationNotFoundException;
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
        JSONObject json = openWeatherClient.getCurrentWeatherByCity("Beijing");

        assertNotNull(json.getString("name"));
        assertNotNull(json.getJSONObject("main").getDouble("temp"));
        assertFalse(json.getJSONArray("weather").isEmpty());
    }

    @Test
    void byCityWithCountry_shouldResolveCountryCode() {
        JSONObject json = openWeatherClient.getCurrentWeatherByCity("London,GB");

        assertEquals("GB", json.getJSONObject("sys").getString("country"));
    }

    @Test
    void byZip_shouldReturnWeather() {
        JSONObject json = openWeatherClient.getCurrentWeatherByZip("10001", "US");

        assertNotNull(json.getString("name"));
        assertNotNull(json.getJSONObject("main").getDouble("temp"));
    }

    @Test
    void byCoordinates_shouldReturnWeather() {
        JSONObject json = openWeatherClient.getCurrentWeatherByCoordinates(39.909, 116.397);

        assertNotNull(json.getJSONObject("coord").getDouble("lat"));
        assertNotNull(json.getJSONObject("coord").getDouble("lon"));
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

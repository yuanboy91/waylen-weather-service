package com.waylen.weather.controller;

import com.alibaba.fastjson.JSON;
import com.waylen.weather.client.OpenWeatherClient;
import com.waylen.weather.model.domain.WeatherResponse;
import com.waylen.weather.model.dto.OpenWeatherDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * WeatherController integration tests.
 * <p>
 * Uses {@code @MockBean} to stub the upstream {@link OpenWeatherClient}
 * so tests run without a real OpenWeatherMap API key.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WeatherControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private OpenWeatherDTO buildMockDTO() {
        OpenWeatherDTO dto = new OpenWeatherDTO();
        dto.setName("Beijing");

        OpenWeatherDTO.Coord coord = new OpenWeatherDTO.Coord();
        coord.setLat(39.9042);
        coord.setLon(116.4074);
        dto.setCoord(coord);

        OpenWeatherDTO.Main main = new OpenWeatherDTO.Main();
        main.setTemp(25.0);
        main.setFeelsLike(26.0);
        main.setTempMin(23.0);
        main.setTempMax(27.0);
        main.setHumidity(60);
        main.setPressure(1013);
        dto.setMain(main);

        OpenWeatherDTO.Weather weather = new OpenWeatherDTO.Weather();
        weather.setMain("Clear");
        weather.setDescription("clear sky");
        weather.setIcon("01d");
        dto.setWeather(Collections.singletonList(weather));

        OpenWeatherDTO.Wind wind = new OpenWeatherDTO.Wind();
        wind.setSpeed(3.5);
        wind.setDeg(180);
        dto.setWind(wind);

        OpenWeatherDTO.Sys sys = new OpenWeatherDTO.Sys();
        sys.setCountry("CN");
        sys.setSunrise(1725500000L);
        sys.setSunset(1725546000L);
        dto.setSys(sys);

        dto.setClouds(new OpenWeatherDTO.Clouds());
        dto.getClouds().setAll(0);
        dto.setVisibility(10000);
        dto.setDt(1725520000L);
        dto.setTimezone(28800);

        return dto;
    }

    // ------------------------------------------------------- /api/weather/city

    @Test
    void getByCity_shouldReturnWeather() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/city?city=Beijing,CN", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        WeatherResponse body = JSON.parseObject(resp.getBody(), WeatherResponse.class);
        assertNotNull(body);
        assertEquals("Beijing", body.getLocationName());
        assertEquals("CN", body.getCountry());
        assertEquals("Clear", body.getCondition());
        assertEquals(25.0, body.getTemperature());
        log.info("[getByCity] response: {}", resp.getBody());
    }

    @Test
    void getByCity_blankCity_shouldReturn400() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/city?city=", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        log.info("[getByCity blank] response: {}", resp.getBody());
    }

    // -------------------------------------------------------- /api/weather/zip

    @Test
    void getByZip_shouldReturnWeather() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/zip?zip=100001&country=CN", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        WeatherResponse body = JSON.parseObject(resp.getBody(), WeatherResponse.class);
        assertNotNull(body);
        assertEquals("Beijing", body.getLocationName());
        log.info("[getByZip] response: {}", resp.getBody());
    }

    @Test
    void getByZip_invalidCountry_shouldReturn400() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/zip?zip=100001&country=china", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        log.info("[getByZip invalid country] response: {}", resp.getBody());
    }

    // ------------------------------------------------ /api/weather/coordinates

    @Test
    void getByCoordinates_shouldReturnWeather() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/coordinates?lat=39.9042&lon=116.4074", String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        WeatherResponse body = JSON.parseObject(resp.getBody(), WeatherResponse.class);
        assertNotNull(body);
        assertNotNull(body.getCoordinates());
        assertEquals(39.9042, body.getCoordinates().getLat());
        assertEquals(116.4074, body.getCoordinates().getLon());
        log.info("[getByCoordinates] response: {}", resp.getBody());
    }

    @Test
    void getByCoordinates_outOfRange_shouldReturn400() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/coordinates?lat=100&lon=200", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        log.info("[getByCoordinates outOfRange] response: {}", resp.getBody());
    }

}

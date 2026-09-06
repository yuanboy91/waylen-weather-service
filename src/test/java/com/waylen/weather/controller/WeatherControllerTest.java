package com.waylen.weather.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waylen.weather.client.OpenWeatherClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HTTP-layer tests for {@link WeatherController}: routing, parameter
 * validation and JSON serialisation, with {@link OpenWeatherClient}
 * stubbed via {@code @MockBean} (no network access required).
 *
 * @author Waylen
 * @date 2026/9/5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WeatherControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private OpenWeatherClient openWeatherClient;

    /**
     * Canonical weather JSON fixture used to stub the client.
     */
    private static JSONObject buildMockJson() {
        return JSON.parseObject("{\"name\":\"Beijing\","
                + "\"coord\":{\"lat\":39.9042,\"lon\":116.4074},"
                + "\"main\":{\"temp\":25.0,\"feels_like\":26.0,\"temp_min\":23.0,\"temp_max\":27.0,"
                + "\"humidity\":60,\"pressure\":1013},"
                + "\"weather\":[{\"main\":\"Clear\",\"description\":\"clear sky\",\"icon\":\"01d\"}],"
                + "\"wind\":{\"speed\":3.5,\"deg\":180},"
                + "\"sys\":{\"country\":\"CN\",\"sunrise\":1725500000,\"sunset\":1725546000},"
                + "\"clouds\":{\"all\":0},\"visibility\":10000,\"dt\":1725520000,\"timezone\":28800}");
    }

    @Test
    void getByCity_shouldReturnWeather() {
        when(openWeatherClient.getCurrentWeatherByCity("Beijing,CN")).thenReturn(buildMockJson());

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/city?city=Beijing,CN", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody())
                .contains("\"locationName\":\"Beijing\"")
                .contains("\"country\":\"CN\"")
                .contains("\"condition\":\"Clear\"")
                .contains("\"temperature\":25.0");
        verify(openWeatherClient).getCurrentWeatherByCity("Beijing,CN");
    }

    @Test
    void getByCity_blankCity_shouldReturn400() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/city?city=", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getByZip_shouldReturnWeather() {
        when(openWeatherClient.getCurrentWeatherByZip("100001", "CN")).thenReturn(buildMockJson());

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/zip?zip=100001&country=CN", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"locationName\":\"Beijing\"");
        verify(openWeatherClient).getCurrentWeatherByZip("100001", "CN");
    }

    @Test
    void getByZip_invalidCountry_shouldReturn400() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/zip?zip=100001&country=china", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getByCoordinates_shouldReturnWeather() {
        when(openWeatherClient.getCurrentWeatherByCoordinates(39.9042, 116.4074))
                .thenReturn(buildMockJson());

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/coordinates?lat=39.9042&lon=116.4074", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody())
                .contains("\"lat\":39.9042")
                .contains("\"lon\":116.4074");
        verify(openWeatherClient).getCurrentWeatherByCoordinates(39.9042, 116.4074);
    }

    @Test
    void getByCoordinates_outOfRange_shouldReturn400() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/weather/coordinates?lat=100&lon=200", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Sanity: all three endpoints route through the @Cacheable service layer
    // (cache behaviour itself is covered by WeatherServiceCacheTest).
    @Test
    void allThreeEndpoints_shouldHitTheServiceLayer() {
        when(openWeatherClient.getCurrentWeatherByCity(anyString())).thenReturn(buildMockJson());
        when(openWeatherClient.getCurrentWeatherByZip(anyString(), anyString())).thenReturn(buildMockJson());
        when(openWeatherClient.getCurrentWeatherByCoordinates(anyDouble(), anyDouble())).thenReturn(buildMockJson());

        restTemplate.getForEntity("/api/weather/city?city=Tokyo", String.class);
        restTemplate.getForEntity("/api/weather/zip?zip=10001&country=US", String.class);
        restTemplate.getForEntity("/api/weather/coordinates?lat=0&lon=0", String.class);

        verify(openWeatherClient).getCurrentWeatherByCity("Tokyo");
        verify(openWeatherClient).getCurrentWeatherByZip("10001", "US");
        verify(openWeatherClient).getCurrentWeatherByCoordinates(0.0, 0.0);
    }

}

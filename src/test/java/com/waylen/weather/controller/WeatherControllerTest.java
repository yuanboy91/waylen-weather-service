package com.waylen.weather.controller;

import com.waylen.weather.client.OpenWeatherClient;
import com.waylen.weather.model.dto.OpenWeatherDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WeatherController HTTP-layer tests.
 * <p>
 * Replaces a previous version that hit the real OpenWeatherMap endpoint
 * (it required a valid API key in the test JVM and was unreliable in CI).
 * This version stubs {@link OpenWeatherClient} via {@code @MockBean}, so the
 * test only exercises:
 * <ul>
 *     <li>URL routing / parameter binding on the controller.</li>
 *     <li>Bean validation on request parameters (returns 400).</li>
 *     <li>JSON serialisation of the domain response.</li>
 * </ul>
 * The client layer itself is covered by
 * {@link com.waylen.weather.client.OpenWeatherClientTest}.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WeatherControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Replaces the real HTTP-calling bean. Anything not stubbed returns
     * Mockito's default (null), which is why each happy-path test must
     * {@code when(...)} the specific method it calls.
     */
    @MockBean
    private OpenWeatherClient openWeatherClient;

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

        OpenWeatherDTO.Clouds clouds = new OpenWeatherDTO.Clouds();
        clouds.setAll(0);
        dto.setClouds(clouds);

        dto.setVisibility(10000);
        dto.setDt(1725520000L);
        dto.setTimezone(28800);
        return dto;
    }

    // ------------------------------------------------------- /api/weather/city

    @Test
    void getByCity_shouldReturnWeather() {
        when(openWeatherClient.getCurrentWeatherByCity("Beijing,CN"))
                .thenReturn(buildMockDTO());

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

    // -------------------------------------------------------- /api/weather/zip

    @Test
    void getByZip_shouldReturnWeather() {
        when(openWeatherClient.getCurrentWeatherByZip("100001", "CN"))
                .thenReturn(buildMockDTO());

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

    // ------------------------------------------------ /api/weather/coordinates

    @Test
    void getByCoordinates_shouldReturnWeather() {
        when(openWeatherClient.getCurrentWeatherByCoordinates(39.9042, 116.4074))
                .thenReturn(buildMockDTO());

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

    // Sanity: all three happy-path stubs get the same DTO — proves we hit the
    // service layer with the @Cacheable annotation in place (cache itself is
    // covered by WeatherServiceCacheTest).
    @Test
    void allThreeEndpoints_useTheSameMockBean() {
        when(openWeatherClient.getCurrentWeatherByCity(anyString())).thenReturn(buildMockDTO());
        when(openWeatherClient.getCurrentWeatherByZip(anyString(), anyString())).thenReturn(buildMockDTO());
        when(openWeatherClient.getCurrentWeatherByCoordinates(anyDouble(), anyDouble())).thenReturn(buildMockDTO());

        restTemplate.getForEntity("/api/weather/city?city=Tokyo", String.class);
        restTemplate.getForEntity("/api/weather/zip?zip=10001&country=US", String.class);
        restTemplate.getForEntity("/api/weather/coordinates?lat=0&lon=0", String.class);

        verify(openWeatherClient).getCurrentWeatherByCity("Tokyo");
        verify(openWeatherClient).getCurrentWeatherByZip("10001", "US");
        verify(openWeatherClient).getCurrentWeatherByCoordinates(0.0, 0.0);
    }
}
package com.waylen.weather.service;

import com.waylen.weather.client.OpenWeatherClient;
import com.waylen.weather.model.domain.WeatherResponse;
import com.waylen.weather.model.dto.OpenWeatherDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cache-behaviour tests for {@link WeatherService}.
 * <p>
 * Verifies that {@code @Cacheable} on the service layer keeps
 * repeated lookups off the upstream HTTP path.
 * <p>
 * Uses {@link MockBean} to stub the upstream {@link OpenWeatherClient}
 * so these tests are fully offline — they do not hit OpenWeatherMap and
 * therefore do <strong>not</strong> require {@code OPENWEATHERMAP_API_KEY}.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@SpringBootTest
@DisplayName("WeatherService cache behaviour")
class WeatherServiceCacheTest {

    @MockBean
    private OpenWeatherClient openWeatherClient;

    @Autowired
    private WeatherService weatherService;

    /**
     * The {@code @Cacheable} proxy returns the cached instance on the second
     * call, which means the same {@link WeatherResponse} object is reused.
     * We use {@code assertSame} rather than {@code assertEquals} to make this
     * explicit (a {@code mockito-wrapped} {@code equals} could otherwise mask
     * a regression where the cache layer accidentally re-creates a new object
     * for every call).
     */
    @Test
    @DisplayName("getCurrentWeatherByCity – second call hits the cache")
    void byCityIsCached() {
        when(openWeatherClient.getCurrentWeatherByCity("London"))
                .thenReturn(stub("London"));

        WeatherResponse first = weatherService.getCurrentWeatherByCity("London");
        WeatherResponse second = weatherService.getCurrentWeatherByCity("London");

        assertNotNull(first);
        assertNotNull(second);
        assertSame(first, second,
                "Cache hit must return the same instance, not a freshly mapped one");
        assertEquals("London", first.getLocationName());
        verify(openWeatherClient, times(1)).getCurrentWeatherByCity("London");
    }

    @Test
    @DisplayName("getCurrentWeatherByZip – second call hits the cache")
    void byZipIsCached() {
        when(openWeatherClient.getCurrentWeatherByZip("10001", "US"))
                .thenReturn(stub("New York"));

        WeatherResponse first = weatherService.getCurrentWeatherByZip("10001", "US");
        WeatherResponse second = weatherService.getCurrentWeatherByZip("10001", "US");

        assertNotNull(first);
        assertNotNull(second);
        assertSame(first, second);
        assertEquals("New York", first.getLocationName());
        verify(openWeatherClient, times(1)).getCurrentWeatherByZip("10001", "US");
    }

    @Test
    @DisplayName("getCurrentWeatherByCoordinates – second call hits the cache")
    void byCoordinatesIsCached() {
        when(openWeatherClient.getCurrentWeatherByCoordinates(48.8566, 2.3522))
                .thenReturn(stub("Paris"));

        WeatherResponse first = weatherService.getCurrentWeatherByCoordinates(48.8566, 2.3522);
        WeatherResponse second = weatherService.getCurrentWeatherByCoordinates(48.8566, 2.3522);

        assertNotNull(first);
        assertNotNull(second);
        assertSame(first, second);
        assertEquals("Paris", first.getLocationName());
        verify(openWeatherClient, times(1))
                .getCurrentWeatherByCoordinates(48.8566, 2.3522);
    }

    /**
     * Distinct cache keys (here: two different cities) must each provoke an
     * upstream call, and must not bleed values into each other.
     * <p>
     * Note: each city is queried only once, so the only way this test can
     * pass is if the cache treats them as separate entries. If "Tokyo" and
     * "Berlin" shared an entry, the second query would return the wrong city
     * name and the assert at the bottom would fail.
     */
    @Test
    @DisplayName("Distinct keys – no false cache sharing")
    void distinctKeysAreNotShared() {
        when(openWeatherClient.getCurrentWeatherByCity("Tokyo"))
                .thenReturn(stub("Tokyo"));
        when(openWeatherClient.getCurrentWeatherByCity("Berlin"))
                .thenReturn(stub("Berlin"));

        WeatherResponse tokyo = weatherService.getCurrentWeatherByCity("Tokyo");
        WeatherResponse berlin = weatherService.getCurrentWeatherByCity("Berlin");

        // Each cache key resolved to its own upstream value.
        assertEquals("Tokyo", tokyo.getLocationName());
        assertEquals("Berlin", berlin.getLocationName());

        // Each upstream call happened exactly once.
        verify(openWeatherClient, times(1)).getCurrentWeatherByCity("Tokyo");
        verify(openWeatherClient, times(1)).getCurrentWeatherByCity("Berlin");
    }

    // --------------------------------------------------------- Test helpers

    /**
     * Builds a minimal {@link OpenWeatherDTO} stub that produces a
     * non-null {@link WeatherResponse} via {@link WeatherService#toWeather} —
     * the test only checks upstream invocation count and the cached
     * instance, so the actual values do not need to be realistic.
     */
    private static OpenWeatherDTO stub(String name) {
        OpenWeatherDTO dto = new OpenWeatherDTO();
        dto.setName(name);
        dto.setDt(1_700_000_000L);
        OpenWeatherDTO.Main main = new OpenWeatherDTO.Main();
        main.setTemp(20.0);
        main.setFeelsLike(19.5);
        main.setTempMin(18.0);
        main.setTempMax(22.0);
        main.setHumidity(60);
        main.setPressure(1010);
        dto.setMain(main);
        OpenWeatherDTO.Weather weather = new OpenWeatherDTO.Weather();
        weather.setMain("Clouds");
        weather.setDescription("few clouds");
        weather.setIcon("02d");
        dto.setWeather(java.util.Collections.singletonList(weather));
        return dto;
    }
}

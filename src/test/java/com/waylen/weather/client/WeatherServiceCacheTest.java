package com.waylen.weather.client;

import com.waylen.weather.model.domain.WeatherResponse;
import com.waylen.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * Verifies that {@code @Cacheable} on {@link WeatherService} keeps repeated
 * lookups off the mocked {@link OpenWeatherClient} — fully offline.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@SpringBootTest
class WeatherServiceCacheTest {

    @MockBean
    private OpenWeatherClient openWeatherClient;

    @Autowired
    private WeatherService weatherService;

    /**
     * Minimal domain stub sufficient for the cache assertions.
     */
    private static WeatherResponse stub(String name) {
        return WeatherResponse.builder()
                .locationName(name)
                .condition("Clouds")
                .description("few clouds")
                .iconCode("02d")
                .temperature(20.0)
                .feelsLike(19.5)
                .humidity(60)
                .pressure(1010)
                .build();
    }

    @Test
    void byCity_secondCallShouldHitCache() {
        when(openWeatherClient.getCurrentWeatherByCity("London")).thenReturn(stub("London"));

        WeatherResponse first = weatherService.getCurrentWeatherByCity("London");
        WeatherResponse second = weatherService.getCurrentWeatherByCity("London");

        assertSame(first, second);
        assertEquals("London", first.getLocationName());
        verify(openWeatherClient, times(1)).getCurrentWeatherByCity("London");
    }

    @Test
    void byZip_secondCallShouldHitCache() {
        when(openWeatherClient.getCurrentWeatherByZip("10001", "US")).thenReturn(stub("New York"));

        WeatherResponse first = weatherService.getCurrentWeatherByZip("10001", "US");
        WeatherResponse second = weatherService.getCurrentWeatherByZip("10001", "US");

        assertSame(first, second);
        assertEquals("New York", first.getLocationName());
        verify(openWeatherClient, times(1)).getCurrentWeatherByZip("10001", "US");
    }

    @Test
    void byCoordinates_secondCallShouldHitCache() {
        when(openWeatherClient.getCurrentWeatherByCoordinates(48.8566, 2.3522))
                .thenReturn(stub("Paris"));

        WeatherResponse first = weatherService.getCurrentWeatherByCoordinates(48.8566, 2.3522);
        WeatherResponse second = weatherService.getCurrentWeatherByCoordinates(48.8566, 2.3522);

        assertSame(first, second);
        assertEquals("Paris", first.getLocationName());
        verify(openWeatherClient, times(1)).getCurrentWeatherByCoordinates(48.8566, 2.3522);
    }

    @Test
    void distinctKeys_shouldBeCachedSeparately() {
        when(openWeatherClient.getCurrentWeatherByCity("Tokyo")).thenReturn(stub("Tokyo"));
        when(openWeatherClient.getCurrentWeatherByCity("Berlin")).thenReturn(stub("Berlin"));

        assertEquals("Tokyo", weatherService.getCurrentWeatherByCity("Tokyo").getLocationName());
        assertEquals("Berlin", weatherService.getCurrentWeatherByCity("Berlin").getLocationName());

        verify(openWeatherClient, times(1)).getCurrentWeatherByCity("Tokyo");
        verify(openWeatherClient, times(1)).getCurrentWeatherByCity("Berlin");
    }

}

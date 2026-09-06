package com.waylen.weather.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
     * Minimal JSON stub sufficient for the {@link WeatherService} mapping.
     */
    private static JSONObject stub(String name) {
        return JSON.parseObject("{\"name\":\"" + name + "\",\"dt\":1700000000,"
                + "\"main\":{\"temp\":20.0,\"feels_like\":19.5,\"humidity\":60,\"pressure\":1010},"
                + "\"weather\":[{\"main\":\"Clouds\",\"description\":\"few clouds\",\"icon\":\"02d\"}]}");
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

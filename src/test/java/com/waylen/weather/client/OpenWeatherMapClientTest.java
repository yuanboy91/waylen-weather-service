package com.waylen.weather.client;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * OpenWeatherMap client exploration test
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@SpringBootTest
public class OpenWeatherMapClientTest {

    @Autowired
    private OpenWeatherMapClient openWeatherMapClient;

    /**
     * Tests fetching current weather by city name.
     */
    @Test
    public void testGWeatherByCity() {
        log.info(JSON.toJSONString(openWeatherMapClient.getCurrentWeatherByCity("Beijing")));
    }

    /**
     * Tests fetching current weather by ZIP code.
     */
    @Test
    public void testGWeatherByZip() {
        log.info(JSON.toJSONString(openWeatherMapClient.getCurrentWeatherByZip("10001", "US")));
    }

    /**
     * Tests fetching current weather by geographic coordinates.
     */
    @Test
    public void testGWeatherByCoordinates() {
        log.info(JSON.toJSONString(openWeatherMapClient.getCurrentWeatherByCoordinates(39.909, 116.397)));
    }

}

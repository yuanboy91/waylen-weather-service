package com.waylen.weather;


import org.springframework.boot.SpringApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the weather service.
 *
 * <p>Exposes current-weather lookup by city name, ZIP code, or geographic
 * coordinates, backed by the OpenWeatherMap API.</p>
 *
 * @author Waylen
 * @date 2026/9/5
 */
@EnableRetry
@SpringBootApplication
public class WeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherApplication.class, args);
    }

}

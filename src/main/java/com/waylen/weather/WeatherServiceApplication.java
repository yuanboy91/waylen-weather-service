package com.waylen.weather;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Weather Service.
 *
 * <p>Exposes current-weather lookup by city name, ZIP code, or geographic
 * coordinates, backed by the OpenWeatherMap API.</p>
 */
@SpringBootApplication
public class WeatherServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherServiceApplication.class, args);
    }

}

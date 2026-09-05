package com.waylen.weather.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for the OpenWeatherMap upstream API.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "openweathermap.api")
public class OpenWeatherMapProperties {

    /**
     * Base URL of the OpenWeatherMap Current Weather Data API
     */
    private String baseUrl = "https://api.openweathermap.org/data/2.5";

    /**
     * API key (appid)
     */
    private String key;

    /**
     * Units for temperature/wind values: metric | imperial | standard
     */
    private String units = "metric";

    /**
     * TCP connect timeout (ms) when calling the upstream API
     */
    private int connectTimeoutMs = 3000;

    /**
     * Socket read timeout (ms) when calling the upstream API
     */
    private int readTimeoutMs = 5000;

}

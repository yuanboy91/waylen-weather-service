package com.waylen.weather.client;

import com.waylen.weather.dto.OpenWeatherMapResponse;

/**
 * Thin abstraction over the OpenWeatherMap Current Weather Data API.
 *
 * @author Waylen
 * @date 2026/9/5
 */
public interface OpenWeatherMapClient {

    /**
     * Fetches current weather by city name, e.g. {@code "London"}.
     *
     * @param city city name, optionally with a country code (e.g. {@code "London,GB"})
     * @return the raw upstream response
     */
    OpenWeatherMapResponse getCurrentWeatherByCity(String city);

    /**
     * Fetches current weather by ZIP code.
     *
     * @param zip         ZIP/postal code, e.g. {@code "10001"}
     * @param countryCode ISO 3166-1 alpha-2 country code, e.g. {@code "US"}
     * @return the raw upstream response
     */
    OpenWeatherMapResponse getCurrentWeatherByZip(String zip, String countryCode);

    /**
     * Fetches current weather by geographic coordinates.
     *
     * @param lat latitude, [-90, 90]
     * @param lon longitude, [-180, 180]
     * @return the raw upstream response
     */
    OpenWeatherMapResponse getCurrentWeatherByCoordinates(double lat, double lon);

}

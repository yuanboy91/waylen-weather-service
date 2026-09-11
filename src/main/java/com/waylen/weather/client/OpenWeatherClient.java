package com.waylen.weather.client;

import com.waylen.weather.model.domain.WeatherResponse;

/**
 * Thin abstraction over the OpenWeatherMap Current Weather Data API.
 *
 * @author Waylen
 * @date 2026/9/5
 */
public interface OpenWeatherClient {

    /**
     * Fetches current weather by city name, e.g. {@code "London"}.
     *
     * @param city city name, optionally with a country code (e.g. {@code "London,GB"})
     * @return the raw upstream response
     */
    WeatherResponse getCurrentWeatherByCity(String city);

    /**
     * Fetches current weather by ZIP code.
     *
     * @param zip         ZIP/postal code, e.g. {@code "10001"}
     * @param countryCode ISO 3166-1 alpha-2 country code, e.g. {@code "US"}
     * @return the raw upstream response
     */
    WeatherResponse getCurrentWeatherByZip(String zip, String countryCode);

    /**
     * Fetches current weather by geographic coordinates.
     *
     * @param lat latitude, [-90, 90]
     * @param lon longitude, [-180, 180]
     * @return the raw upstream response
     */
    WeatherResponse getCurrentWeatherByCoordinates(double lat, double lon);

    /**
     * Fetches current weather by OpenWeatherMap city ID.
     *
     * <p>The city ID uniquely identifies a city in the OpenWeatherMap
     * database (e.g. {@code 5909629}), so lookups by ID never suffer from
     * duplicate city names. IDs can be obtained from the official city list
     * or from the {@code id} field of any weather response.</p>
     *
     * @param cityId OpenWeatherMap city ID, digits only
     * @return the raw upstream response
     */
    WeatherResponse getCurrentWeatherByCityId(String cityId);

}

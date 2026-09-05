package com.waylen.weather.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Domain model representing the current weather for a location.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherResponse implements Serializable {

    /**
     * Resolved location name reported by the upstream provider.
     */
    private String locationName;

    /**
     * ISO 3166-1 alpha-2 country code (e.g. "US", "GB"), may be null.
     */
    private String country;

    /**
     * Geographic coordinates the weather was queried for.
     */
    private Coordinates coordinates;

    /**
     * Short weather condition label, e.g. "Rain", "Clouds", "Clear".
     */
    private String condition;

    /**
     * Long-form human-readable description, e.g. "light rain".
     */
    private String description;

    /**
     * OpenWeatherMap icon code (e.g. "10d") that can be mapped to an icon URL.
     */
    private String iconCode;

    /**
     * Current temperature in the configured units.
     */
    private Double temperature;

    /**
     * "Feels like" temperature in the configured units.
     */
    private Double feelsLike;

    /**
     * Minimum temperature currently observed.
     */
    private Double tempMin;

    /**
     * Maximum temperature currently observed.
     */
    private Double tempMax;

    /**
     * Relative humidity percentage (0-100).
     */
    private Integer humidity;

    /**
     * Atmospheric pressure in hPa.
     */
    private Integer pressure;

    /**
     * Wind information.
     */
    private Wind wind;

    /**
     * Cloudiness percentage (0-100).
     */
    private Integer cloudiness;

    /**
     * Visibility in meters.
     */
    private Integer visibility;

    /**
     * Data calculation time as a Unix epoch (seconds).
     */
    private Long timestamp;

    /**
     * Sunrise time as a Unix epoch (seconds).
     */
    private Long sunrise;

    /**
     * Sunset time as a Unix epoch (seconds).
     */
    private Long sunset;

    /**
     * Timezone offset from UTC in seconds.
     */
    private Integer timezoneOffset;

    /**
     * Geographic coordinates.
     */
    @Data
    @Builder
    public static class Coordinates implements Serializable {
        private Double lat;
        private Double lon;
    }

    /**
     * Wind information.
     */
    @Data
    @Builder
    public static class Wind implements Serializable {
        private Double speed;
        private Integer degree;
        private Double gust;
    }

}

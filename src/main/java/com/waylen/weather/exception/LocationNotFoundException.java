package com.waylen.weather.exception;

import lombok.Getter;

/**
 * Thrown when OpenWeatherMap reports that the requested location
 * (city name, ZIP code, or coordinates) does not exist.
 * The service layer translates this into a 404 for our own API consumers.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Getter
public class LocationNotFoundException extends RuntimeException {

    /**
     * The original location query that could not be resolved
     */
    private final String location;

    public LocationNotFoundException(String location, Throwable cause) {
        super("Location not found: " + location, cause);
        this.location = location;
    }

}

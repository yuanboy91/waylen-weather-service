package com.waylen.weather.exception;

/**
 * Thrown when a call to OpenWeatherMap fails:
 * upstream 4xx/5xx responses, timeouts, or network errors.
 *
 * @author Waylen
 * @date 2026/9/5
 */
public class OpenWeatherMapException extends RuntimeException {

    public OpenWeatherMapException(String message) {
        super(message);
    }

    public OpenWeatherMapException(String message, Throwable cause) {
        super(message, cause);
    }

}

package com.waylen.weather.exception;

/**
 * Thrown when the rate limiter rejects a request.
 *
 * @author Waylen
 * @date 2026/9/6
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }

}

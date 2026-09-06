package com.waylen.weather.exception;

import com.waylen.weather.model.domain.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * Global exception handler that translates exceptions into uniform {@link ApiResponse}.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleLocationNotFound(LocationNotFoundException ex,
                                                                   HttpServletRequest request) {
        log.warn("Location not found: {}", ex.getMessage());
        return respond(HttpStatus.NOT_FOUND, "LOCATION_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(OpenWeatherMapException.class)
    public ResponseEntity<ApiResponse<?>> handleUpstream(OpenWeatherMapException ex,
                                                           HttpServletRequest request) {
        log.error("Upstream failure: {}", ex.getMessage(), ex);
        return respond(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR",
                "Failed to reach the weather provider, please retry later.", request);
    }

    /**
     * Maps bean-validation failures on request parameters to 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(ConstraintViolationException ex,
                                                            HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("Invalid request parameters: {}", message);
        return respond(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected server error, please retry later.", request);
    }

    private ResponseEntity<ApiResponse<?>> respond(HttpStatus status, String code,
                                                     String message, HttpServletRequest request) {
        ApiResponse<?> body = ApiResponse.error(code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

}

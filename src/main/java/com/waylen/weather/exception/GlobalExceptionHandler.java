package com.waylen.weather.exception;

import com.waylen.weather.model.domain.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * Translates application and validation exceptions into uniform HTTP
 * responses with {@link ApiErrorResponse} bodies.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLocationNotFound(LocationNotFoundException ex,
                                                                   HttpServletRequest request) {
        log.warn("Location not found for request {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build("LOCATION_NOT_FOUND", ex.getMessage(), request));
    }

    @ExceptionHandler(OpenWeatherMapException.class)
    public ResponseEntity<ApiErrorResponse> handleUpstream(OpenWeatherMapException ex,
                                                           HttpServletRequest request) {
        log.error("OpenWeatherMap upstream failure for request {}: {}",
                request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(build("UPSTREAM_ERROR",
                        "Failed to reach the weather provider, please retry later.",
                        request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ConstraintViolationException ex,
                                                             HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(this::formatViolation)
                .findFirst()
                .orElse(ex.getMessage());
        log.info("Invalid request {}: {}", request.getRequestURI(), detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build("INVALID_REQUEST", detail, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for request {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("INTERNAL_ERROR",
                        "Unexpected server error, please retry later.",
                        request));
    }

    private ApiErrorResponse build(String code, String message, HttpServletRequest request) {
        return ApiErrorResponse.builder()
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        // Strip "methodName.argName" prefix produced by @Validated on controllers.
        int dot = path.lastIndexOf('.');
        String field = dot < 0 ? path : path.substring(dot + 1);
        return field + ": " + violation.getMessage();
    }

}

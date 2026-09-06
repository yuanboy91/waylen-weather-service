package com.waylen.weather.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Unified API response wrapper for both success and error paths.
 *
 * @param <T> response payload type; {@link Void} for error-only responses
 * @author Waylen
 * @date 2026/9/5
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    /** Machine-readable result code, e.g. "OK", "LOCATION_NOT_FOUND". */
    private String code;

    /** Human-readable message describing the result. */
    private String message;

    /** Response payload on success; {@code null} on error. */
    private T data;

    /** Request path for diagnostics; only set on error. */
    private String path;

    /** Epoch milliseconds when the response was created. */
    private Long timestamp;

    // --------------- factory methods ---------------

    /** Build a success response. */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("OK")
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** Build an error response. */
    public static <T> ApiResponse<T> error(String code, String message, String path) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(System.currentTimeMillis())
                .build();
    }

}

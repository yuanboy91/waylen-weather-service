package com.waylen.weather.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Standard error response body returned by every error path.
 *
 * <p>Stays small on purpose so clients can rely on a stable shape regardless
 * of which endpoint or upstream produced the failure.</p>
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse implements Serializable {

    /**
     * Machine-readable error code, e.g. "LOCATION_NOT_FOUND".
     */
    private String code;

    /**
     * Human-readable error message safe to surface to end users.
     */
    private String message;

    /**
     * Optional request path that produced the error, for diagnostics.
     */
    private String path;

    /**
     * Epoch milliseconds when the error occurred on the server.
     */
    private Long timestamp;

}

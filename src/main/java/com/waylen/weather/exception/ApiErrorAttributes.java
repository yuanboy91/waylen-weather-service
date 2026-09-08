package com.waylen.weather.exception;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maintain consistency in business response objects
 *
 * @author Waylen
 * @date 2026/9/8
 */
@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    private static final String ATTR_STATUS_CODE = "javax.servlet.error.status_code";
    private static final String ATTR_REQUEST_URI = "javax.servlet.error.request_uri";

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                  ErrorAttributeOptions options) {
        HttpStatus status = resolveStatus(webRequest);
        Throwable error = getError(webRequest);

        Map<String, Object> attrs = new LinkedHashMap<>(4);
        attrs.put("code", status == HttpStatus.INTERNAL_SERVER_ERROR ? "INTERNAL_ERROR" : status.name());
        attrs.put("message", resolveMessage(error, status));
        attrs.put("path", resolvePath(webRequest));
        attrs.put("timestamp", System.currentTimeMillis());
        return attrs;
    }

    private static HttpStatus resolveStatus(WebRequest req) {
        Integer sc = (Integer) req.getAttribute(ATTR_STATUS_CODE, WebRequest.SCOPE_REQUEST);
        return sc != null && HttpStatus.resolve(sc) != null
                ? HttpStatus.resolve(sc)
                : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String resolveMessage(Throwable error, HttpStatus status) {
        if (error instanceof NoHandlerFoundException) {
            NoHandlerFoundException ex = (NoHandlerFoundException) error;
            return "No handler for " + ex.getHttpMethod() + " " + ex.getRequestURL();
        }
        if (error != null && StringUtils.isNotEmpty(error.getMessage())) {
            return error.getMessage();
        }
        return status.getReasonPhrase();
    }

    private static String resolvePath(WebRequest req) {
        Object uri = req.getAttribute(ATTR_REQUEST_URI, WebRequest.SCOPE_REQUEST);
        if (uri instanceof String) {
            return (String) uri;
        }
        return ((ServletWebRequest) req).getRequest().getRequestURI();
    }

}
package com.waylen.weather.interceptor;

import com.google.common.util.concurrent.RateLimiter;
import com.waylen.weather.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rejected requests throw {@link RateLimitExceededException}, which the global
 * exception handler maps to HTTP 429 with the unified error body.
 *
 * @author Waylen
 * @date 2026/9/6
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimiter.tryAcquire()) {
            log.warn("Rate limit exceeded for {} {}", request.getMethod(), request.getRequestURI());
            throw new RateLimitExceededException(
                    "Too many requests, please retry later (client " + request.getRemoteAddr() + ")");
        }
        return true;
    }

}

package com.waylen.weather.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

/**
 * Logs spring-retry lifecycle events for the upstream OpenWeatherMap calls.
 *
 * @author Waylen
 * @date 2026/9/6
 */
@Slf4j
@Component
public class RetryLoggingListener implements RetryListener {

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        return true;
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.info("Upstream OpenWeatherMap call failed on attempt {} (will retry): {}",
                context.getRetryCount(), throwable.getMessage());
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable == null) {
            log.info("Upstream OpenWeatherMap call succeeded after {} attempt(s)", context.getRetryCount());
        } else {
            log.error("Upstream OpenWeatherMap call failed after {} attempt(s), giving up: {}",
                    context.getRetryCount(), throwable.getMessage());
        }
    }

}

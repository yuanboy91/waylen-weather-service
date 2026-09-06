package com.waylen.weather.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Cache decorator that logs every read as a hit or a miss.
 *
 * @author Waylen
 * @date 2026/9/6
 */
@Slf4j
public class LoggingCache implements Cache {

    private final Cache delegate;

    public LoggingCache(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper value = delegate.get(key);
        logHitOrMiss(key, value != null);
        return value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = delegate.get(key, type);
        logHitOrMiss(key, value != null);
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper existing = delegate.get(key);
        if (existing != null) {
            logHitOrMiss(key, true);
            return (T) existing.get();
        }
        logHitOrMiss(key, false);
        return delegate.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    private void logHitOrMiss(Object key, boolean hit) {
        if (hit) {
            log.info("Cache HIT on '{}' for key '{}'", getName(), key);
        } else {
            log.info("Cache MISS on '{}' for key '{}'", getName(), key);
        }
    }

}

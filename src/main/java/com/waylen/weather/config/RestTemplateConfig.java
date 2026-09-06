package com.waylen.weather.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared {@link RestTemplate} bean with timeouts and common request headers.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     OpenWeatherProperties properties) {
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        // Ensure UTF-8 encoding for all outbound requests
        interceptors.add((request, body, execution) -> {
            request.getHeaders().set("Accept-Charset", StandardCharsets.UTF_8.name());
            return execution.execute(request, body);
        });

        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .additionalInterceptors(interceptors)
                .build();
    }

}

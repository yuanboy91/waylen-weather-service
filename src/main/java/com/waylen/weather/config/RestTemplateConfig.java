package com.waylen.weather.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

/**
 * Centralised HTTP client configuration for every outbound {@link RestTemplate}
 * in the application.
 * <p>
 * Spring Boot 2.3 auto-configures a {@link RestTemplateBuilder} for
 * dependency injection; this configuration consumes that builder and applies
 * cross-cutting concerns:
 * <ul>
 *     <li>Connection and read timeouts (driven by {@link OpenWeatherProperties}).</li>
 *     <li>A stable {@code User-Agent} header on every request, so external
 *         providers and any future API gateway in front of them can identify
 *         the caller instead of seeing the default
 *         {@code "Apache-HttpClient/4.5.x (Java/1.8.0_xxx)"} string.</li>
 * </ul>
 * Adding a new outbound HTTP client does not require touching this file:
 * inject the shared {@link RestTemplate} bean and use it directly.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    /** Default User-Agent for every outgoing request. */
    public static final String USER_AGENT =
            "WaylenWeatherService/1.0 (+github.com/yuanboy91/waylen-weather-service)";

    /**
     * Shared {@link RestTemplate} used by every outbound HTTP client.
     * <p>
     * Per-client timeouts come from {@link OpenWeatherProperties} so they can
     * be tuned per environment (e.g. a longer timeout for batch jobs) without
     * code changes.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     OpenWeatherProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .additionalInterceptors(new UserAgentInterceptor(USER_AGENT))
                .build();
    }

    /**
     * {@link ClientHttpRequestInterceptor} that sets the {@code User-Agent}
     * header on every outgoing request.
     */
    static final class UserAgentInterceptor implements ClientHttpRequestInterceptor {

        private final String userAgent;

        UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request,
                                            byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set("User-Agent", userAgent);
            if (log.isDebugEnabled()) {
                log.debug("Outbound {} {} with User-Agent={}",
                        request.getMethod(), request.getURI(), userAgent);
            }
            return execution.execute(request, body);
        }
    }
}

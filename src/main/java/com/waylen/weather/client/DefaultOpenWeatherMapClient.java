package com.waylen.weather.client;

import com.waylen.weather.config.OpenWeatherMapProperties;
import com.waylen.weather.dto.OpenWeatherMapResponse;
import com.waylen.weather.exception.LocationNotFoundException;
import com.waylen.weather.exception.OpenWeatherMapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * {@link RestTemplate}-based implementation of {@link OpenWeatherMapClient}.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Component
public class DefaultOpenWeatherMapClient implements OpenWeatherMapClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultOpenWeatherMapClient.class);

    private final RestTemplate restTemplate;
    private final OpenWeatherMapProperties properties;

    public DefaultOpenWeatherMapClient(RestTemplateBuilder restTemplateBuilder,
                                       OpenWeatherMapProperties properties) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }

    @Override
    public OpenWeatherMapResponse getCurrentWeatherByCity(String city) {
        URI uri = baseUri()
                .queryParam("q", city)
                .build()
                .toUri();
        return fetch(uri, "city=" + city);
    }

    @Override
    public OpenWeatherMapResponse getCurrentWeatherByZip(String zip, String countryCode) {
        // The upstream API expects "zip={zip},{country}", e.g. zip=10001,US
        URI uri = baseUri()
                .queryParam("zip", zip + "," + countryCode)
                .build()
                .toUri();
        return fetch(uri, "zip=" + zip + "," + countryCode);
    }

    @Override
    public OpenWeatherMapResponse getCurrentWeatherByCoordinates(double lat, double lon) {
        URI uri = baseUri()
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .build()
                .toUri();
        return fetch(uri, "lat=" + lat + ",lon=" + lon);
    }

    private UriComponentsBuilder baseUri() {
        return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/weather")
                .queryParam("appid", properties.getKey())
                .queryParam("units", properties.getUnits());
    }

    private OpenWeatherMapResponse fetch(URI uri, String queryDescription) {
        log.debug("Calling OpenWeatherMap: query={}", queryDescription);
        try {
            return restTemplate.getForObject(uri, OpenWeatherMapResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new LocationNotFoundException(queryDescription, e);
            }
            // e.g. 401 (invalid API key), 429 (rate limited)
            throw new OpenWeatherMapException(
                    "OpenWeatherMap rejected the request for " + queryDescription
                            + ": HTTP " + e.getRawStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (HttpStatusCodeException e) {
            // Upstream 5xx
            throw new OpenWeatherMapException(
                    "OpenWeatherMap error for " + queryDescription
                            + ": HTTP " + e.getRawStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            // Connect/read timeout or network failure
            throw new OpenWeatherMapException(
                    "Failed to reach OpenWeatherMap for " + queryDescription
                            + ": " + e.getMessage(), e);
        }
    }

}

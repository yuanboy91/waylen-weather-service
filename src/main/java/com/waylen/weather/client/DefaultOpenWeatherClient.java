package com.waylen.weather.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waylen.weather.config.OpenWeatherProperties;
import com.waylen.weather.exception.LocationNotFoundException;
import com.waylen.weather.exception.OpenWeatherMapException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * OpenWeatherMap client implementation using shared {@link RestTemplate}.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@Component
public class DefaultOpenWeatherClient implements OpenWeatherClient {

    private final RestTemplate restTemplate;
    private final OpenWeatherProperties properties;

    public DefaultOpenWeatherClient(RestTemplate restTemplate,
                                    OpenWeatherProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public JSONObject getCurrentWeatherByCity(String city) {
        URI uri = baseUri()
                .queryParam("q", city)
                .build()
                .toUri();
        return fetch(uri, "city=" + city);
    }

    @Override
    public JSONObject getCurrentWeatherByZip(String zip, String countryCode) {
        // The upstream API expects "zip={zip},{country}", e.g. zip=10001,US
        URI uri = baseUri()
                .queryParam("zip", zip + "," + countryCode)
                .build()
                .toUri();
        return fetch(uri, "zip=" + zip + "," + countryCode);
    }

    @Override
    public JSONObject getCurrentWeatherByCoordinates(double lat, double lon) {
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

    private JSONObject fetch(URI uri, String queryDescription) {
        log.debug("Calling OpenWeatherMap: query={}", queryDescription);
        try {
            String body = restTemplate.getForObject(uri, String.class);
            return JSON.parseObject(body);
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

package com.waylen.weather.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.waylen.weather.config.OpenWeatherProperties;
import com.waylen.weather.exception.LocationNotFoundException;
import com.waylen.weather.exception.OpenWeatherMapException;
import com.waylen.weather.model.domain.WeatherResponse;
import com.waylen.weather.model.domain.WeatherResponse.Wind;
import com.waylen.weather.model.dto.OpenWeatherDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

    @Retryable(
            include = OpenWeatherMapException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    @Override
    public WeatherResponse getCurrentWeatherByCity(String city) {
        URI uri = baseUri()
                .queryParam("q", city)
                .build()
                .toUri();
        return toWeather(fetch(uri, "city=" + city));
    }

    @Retryable(
            include = OpenWeatherMapException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    @Override
    public WeatherResponse getCurrentWeatherByZip(String zip, String countryCode) {
        // The upstream API expects "zip={zip},{country}", e.g. zip=10001,US
        URI uri = baseUri()
                .queryParam("zip", zip + "," + countryCode)
                .build()
                .toUri();
        return toWeather(fetch(uri, "zip=" + zip + "," + countryCode));
    }

    @Retryable(
            include = OpenWeatherMapException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    @Override
    public WeatherResponse getCurrentWeatherByCoordinates(double lat, double lon) {
        URI uri = baseUri()
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .build()
                .toUri();
        return toWeather(fetch(uri, "lat=" + lat + ",lon=" + lon));
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
                // Deterministic failure, retry meaningless
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

    /**
     * Map provider JSON to domain model via DTO, null-safe on all sections.
     */
    private WeatherResponse toWeather(JSONObject json) {
        OpenWeatherDTO dto = json.toJavaObject(OpenWeatherDTO.class);

        OpenWeatherDTO.Wind windDto = dto.getWind();
        Wind wind = (windDto != null)
                ? Wind.builder().speed(windDto.getSpeed()).degree(windDto.getDeg()).gust(windDto.getGust()).build()
                : null;

        OpenWeatherDTO.Main main = dto.getMain();
        List<OpenWeatherDTO.Weather> conditions = dto.getWeather();
        OpenWeatherDTO.Weather weather = (conditions != null && !conditions.isEmpty()) ? conditions.get(0) : null;

        OpenWeatherDTO.Sys sys = dto.getSys();

        return WeatherResponse.builder()
                .locationName(dto.getName())
                .country(sys != null ? sys.getCountry() : null)
                .condition(weather != null ? weather.getMain() : null)
                .description(weather != null ? weather.getDescription() : null)
                .iconCode(weather != null ? weather.getIcon() : null)
                .temperature(main != null ? main.getTemp() : null)
                .feelsLike(main != null ? main.getFeelsLike() : null)
                .humidity(main != null ? main.getHumidity() : null)
                .pressure(main != null ? main.getPressure() : null)
                .wind(wind)
                .cloudiness(dto.getClouds() != null ? dto.getClouds().getAll() : null)
                .visibility(dto.getVisibility())
                .build();
    }

}

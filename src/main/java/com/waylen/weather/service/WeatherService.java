package com.waylen.weather.service;

import com.waylen.weather.client.OpenWeatherClient;
import com.waylen.weather.model.domain.WeatherResponse;
import com.waylen.weather.model.domain.WeatherResponse.Coordinates;
import com.waylen.weather.model.domain.WeatherResponse.Wind;
import com.waylen.weather.model.dto.OpenWeatherDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service that orchestrates weather lookups.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Delegate the upstream HTTP call to {@link OpenWeatherClient}.</li>
 *   <li>Map the third-party wire format ({@link OpenWeatherDTO})
 *       into our own {@link WeatherResponse} domain model.</li>
 *   <li>Log every call with its query parameters for traceability.</li>
 * </ul>
 *
 * <p>Exceptions raised by the client layer ({@code LocationNotFoundException},
 * {@code OpenWeatherMapException}) are propagated unchanged so the controller
 * layer can translate them into HTTP responses.</p>
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final OpenWeatherClient openWeatherClient;

    /**
     * Look up current weather by city name.
     *
     * @param city city name, optionally suffixed with a country code
     *             (e.g. "London" or "London,GB")
     * @return mapped weather domain model
     */
    public WeatherResponse getCurrentWeatherByCity(String city) {
        log.info("Querying weather by city: {}", city);
        OpenWeatherDTO response = openWeatherClient.getCurrentWeatherByCity(city);
        return toWeather(response);
    }

    /**
     * Look up current weather by ZIP/postal code.
     *
     * @param zip         ZIP or postal code
     * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "US")
     * @return mapped weather domain model
     */
    public WeatherResponse getCurrentWeatherByZip(String zip, String countryCode) {
        log.info("Querying weather by ZIP: {}/{}", zip, countryCode);
        OpenWeatherDTO response = openWeatherClient.getCurrentWeatherByZip(zip, countryCode);
        return toWeather(response);
    }

    /**
     * Look up current weather by geographic coordinates.
     *
     * @param lat latitude in the range [-90, 90]
     * @param lon longitude in the range [-180, 180]
     * @return mapped weather domain model
     */
    public WeatherResponse getCurrentWeatherByCoordinates(double lat, double lon) {
        log.info("Querying weather by coordinates: lat={}, lon={}", lat, lon);
        OpenWeatherDTO response = openWeatherClient.getCurrentWeatherByCoordinates(lat, lon);
        return toWeather(response);
    }

    /**
     * Map the upstream wire format to our own {@link WeatherResponse} domain model.
     *
     * <p>Defensive against null upstream sections: missing {@code main},
     * {@code weather}, {@code wind}, etc. yield nulls on the domain side
     * rather than blowing up the request.</p>
     */
    private WeatherResponse toWeather(OpenWeatherDTO upstream) {
        Coordinates coordinates = null;
        if (upstream.getCoord() != null
                && upstream.getCoord().getLat() != null
                && upstream.getCoord().getLon() != null) {
            coordinates = Coordinates.builder()
                    .lat(upstream.getCoord().getLat())
                    .lon(upstream.getCoord().getLon())
                    .build();
        }

        Wind wind = null;
        if (upstream.getWind() != null) {
            wind = Wind.builder()
                    .speed(upstream.getWind().getSpeed())
                    .degree(upstream.getWind().getDeg())
                    .gust(upstream.getWind().getGust())
                    .build();
        }

        OpenWeatherDTO.Main main = upstream.getMain();
        List<OpenWeatherDTO.Weather> conditions = upstream.getWeather();
        OpenWeatherDTO.Weather firstCondition = (conditions == null || conditions.isEmpty())
                ? null : conditions.get(0);

        String country = null;
        Long sunrise = null;
        Long sunset = null;
        if (upstream.getSys() != null) {
            country = upstream.getSys().getCountry();
            sunrise = upstream.getSys().getSunrise();
            sunset = upstream.getSys().getSunset();
        }

        return WeatherResponse.builder()
                .locationName(upstream.getName())
                .country(country)
                .coordinates(coordinates)
                .condition(firstCondition == null ? null : firstCondition.getMain())
                .description(firstCondition == null ? null : firstCondition.getDescription())
                .iconCode(firstCondition == null ? null : firstCondition.getIcon())
                .temperature(main == null ? null : main.getTemp())
                .feelsLike(main == null ? null : main.getFeelsLike())
                .tempMin(main == null ? null : main.getTempMin())
                .tempMax(main == null ? null : main.getTempMax())
                .humidity(main == null ? null : main.getHumidity())
                .pressure(main == null ? null : main.getPressure())
                .wind(wind)
                .cloudiness(upstream.getClouds() == null ? null : upstream.getClouds().getAll())
                .visibility(upstream.getVisibility())
                .timestamp(upstream.getDt())
                .sunrise(sunrise)
                .sunset(sunset)
                .timezoneOffset(upstream.getTimezone())
                .build();
    }

}

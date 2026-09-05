package com.waylen.weather.controller;

import com.waylen.weather.model.domain.WeatherResponse;
import com.waylen.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * REST API exposing current-weather lookups.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Validated
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * Look up current weather by city name.
     *
     * @param city city name, optionally suffixed with ",{countryCode}"
     * @return weather for the resolved location
     */
    @GetMapping("/city")
    public WeatherResponse getByCity(@RequestParam("city")
                             @NotBlank(message = "city must not be blank") String city) {
        return weatherService.getCurrentWeatherByCity(city);
    }

    /**
     * Look up current weather by ZIP/postal code.
     *
     * @param zip     ZIP or postal code (1-10 alphanumeric/hyphen characters)
     * @param country ISO 3166-1 alpha-2 country code, e.g. "US"
     * @return weather for the resolved location
     */
    @GetMapping("/zip")
    public WeatherResponse getByZip(
            @RequestParam("zip")
            @NotBlank(message = "zip must not be blank")
            @Pattern(regexp = "^[A-Za-z0-9\\- ]{1,10}$",
                    message = "zip must be 1-10 letters, digits, spaces or hyphens")
            String zip,

            @RequestParam("country")
            @NotBlank(message = "country must not be blank")
            @Pattern(regexp = "^[A-Z]{2}$",
                    message = "country must be a 2-letter ISO code in upper case")
            String country) {
        return weatherService.getCurrentWeatherByZip(zip, country);
    }

    /**
     * Look up current weather by geographic coordinates.
     *
     * @param lat latitude in the range [-90, 90]
     * @param lon longitude in the range [-180, 180]
     * @return weather for the resolved location
     */
    @GetMapping("/coordinates")
    public WeatherResponse getByCoordinates(
            @RequestParam("lat")
            @DecimalMin(value = "-90", message = "lat must be >= -90")
            @DecimalMax(value = "90", message = "lat must be <= 90")
            double lat,

            @RequestParam("lon")
            @DecimalMin(value = "-180", message = "lon must be >= -180")
            @DecimalMax(value = "180", message = "lon must be <= 180")
            double lon) {
        return weatherService.getCurrentWeatherByCoordinates(lat, lon);
    }

}

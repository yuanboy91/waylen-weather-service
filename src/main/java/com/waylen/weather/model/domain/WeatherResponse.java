package com.waylen.weather.model.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Current-weather response exposed to API consumers.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherResponse implements Serializable {

    private String locationName;
    private String country;
    private String condition;
    private String description;
    private String iconCode;
    private Double temperature;
    private Double feelsLike;
    private Integer humidity;
    private Integer pressure;
    private Wind wind;
    private Integer cloudiness;
    private Integer visibility;

    @Data
    @Builder
    public static class Wind implements Serializable {
        private Double speed;
        private Integer degree;
        private Double gust;
    }

}

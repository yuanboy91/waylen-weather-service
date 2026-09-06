package com.waylen.weather.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Canonical weather schema that every provider implementation
 * normalises its JSON payload into.
 *
 * @author Waylen
 * @date 2026/9/5
 */
@Data
public class OpenWeatherDTO implements Serializable {

    private String name;
    private Coord coord;
    private List<Weather> weather;
    private Main main;
    private Wind wind;
    private Clouds clouds;
    private Sys sys;
    private Integer visibility;
    private Long dt;
    private Integer timezone;

    @Data
    public static class Coord implements Serializable {
        private Double lon;
        private Double lat;
    }

    @Data
    public static class Weather implements Serializable {
        private String main;
        private String description;
        private String icon;
    }

    @Data
    public static class Main implements Serializable {
        private Double temp;

        @JSONField(name = "feels_like")
        private Double feelsLike;

        @JSONField(name = "temp_min")
        private Double tempMin;

        @JSONField(name = "temp_max")
        private Double tempMax;

        private Integer pressure;
        private Integer humidity;
    }

    @Data
    public static class Wind implements Serializable {
        private Double speed;
        private Integer deg;
        private Double gust;
    }

    @Data
    public static class Clouds implements Serializable {
        private Integer all;
    }

    @Data
    public static class Sys implements Serializable {
        private String country;
        private Long sunrise;
        private Long sunset;
    }

}

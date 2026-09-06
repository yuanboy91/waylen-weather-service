package com.waylen.weather.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal OpenAPI metadata for the auto-generated Swagger UI page
 * (springdoc-openapi). Endpoints are discovered automatically.
 *
 * @author Waylen
 * @date 2026/9/6
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weatherOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Waylen Weather Service API")
                        .description("Current weather lookups by city name, ZIP/postal code, "
                                + "or geographic coordinates (OpenWeatherMap).")
                        .version("1.0.0"));
    }

}

package com.duoc.aquasample_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Título y descripción que aparecen en Swagger UI (/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aquaSampleOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AquaSample API")
                .version("1.0")
                .description("Backend de AquaSample (ALDEMAR SpA): registro, conteo y trazabilidad de muestras de choritos."));
    }
}

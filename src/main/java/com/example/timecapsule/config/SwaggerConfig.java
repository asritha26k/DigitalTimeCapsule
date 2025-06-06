package com.example.timecapsule.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info() // Correct usage: create an Info object and then set it on the OpenAPI object
                        .title("Your Application API Title")
                        .version("1.0")
                        .description("API documentation for your Spring Boot application. " +
                                "This description can be more detailed, covering purpose, etc."));
    }
}

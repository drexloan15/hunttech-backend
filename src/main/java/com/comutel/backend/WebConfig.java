package com.comutel.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Aplica a todas las URLs
                        .allowedOrigins("http://localhost:5173") // Permite al Frontend
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Permite estos verbos
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
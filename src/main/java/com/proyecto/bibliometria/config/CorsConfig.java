package com.proyecto.bibliometria.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") String configuredOrigins) {
        this.allowedOrigins = StringUtils.hasText(configuredOrigins)
                ? Arrays.stream(configuredOrigins.split(",")).map(String::trim).filter(StringUtils::hasText).toList()
                : List.of();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!allowedOrigins.isEmpty()) {
            registry.addMapping("/api/**")
                    .allowedOrigins(allowedOrigins.toArray(String[]::new))
                    .allowedMethods("GET", "POST", "OPTIONS")
                    .allowedHeaders("Content-Type", "Authorization")
                    .allowCredentials(false)
                    .maxAge(3600);
        }
    }
}

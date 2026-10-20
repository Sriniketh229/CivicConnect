package com.civicconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the CivicConnect REST API.
 *
 * <p>Allowed origins are read from the {@code app.cors.allowed-origins}
 * property (comma-separated). In development this includes the Vite dev
 * server ({@code http://localhost:5173} and the Figma Make preview port
 * {@code http://localhost:8443}). In production, set this property to the
 * deployed frontend domain only.
 *
 * <p>The bean is consumed by {@link SecurityConfig} which registers it in
 * the Spring Security filter chain via {@code .cors(cors -> cors.configurationSource(...))}.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:8443}")
    private String allowedOriginsRaw;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated origins from property
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));

        // Allow credentials so the frontend can send the Authorization header
        // or cookies (if cookie-based auth is chosen in Phase 2).
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Servlet context path (/api) is excluded before Spring matches this path.
        source.registerCorsConfiguration("/v1/**", config);
        return source;
    }
}

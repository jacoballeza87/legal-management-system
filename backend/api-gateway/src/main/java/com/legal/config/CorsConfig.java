package com.legal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración CORS centralizada.
 *
 * Al estar en el Gateway, todos los microservicios heredan automáticamente
 * esta configuración. Los servicios individuales NO deben configurar CORS.
 *
 * Orígenes permitidos (configurable por entorno):
 *  - Desarrollo: http://localhost:4200 (Angular dev server)
 *  - Producción: dominio real desde variable CORS_ALLOWED_ORIGINS
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:3000}")
    private String allowedOriginsRaw;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ── Orígenes permitidos ───────────────────────────────────────────────
        List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
        config.setAllowedOrigins(origins);

        // ── Métodos permitidos ────────────────────────────────────────────────
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        ));

        // ── Headers permitidos ────────────────────────────────────────────────
        config.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Cache-Control",
            "X-Device-Id",
            "X-Device-Name",
            "X-Device-Type",
            "X-Gateway-Source"
        ));

        // ── Headers expuestos al cliente ──────────────────────────────────────
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Total-Count",
            "X-Total-Pages",
            "Content-Disposition",
            "X-Request-Id"
        ));

        // ── Credenciales (cookies/auth headers) ───────────────────────────────
        config.setAllowCredentials(true);

        // ── Pre-flight cache (1 hora) ─────────────────────────────────────────
        config.setMaxAge(3600L);

        // Aplicar a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}

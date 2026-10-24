package com.legal.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service
                .route("auth_service_route", r -> r.path("/api/v1/auth/**")
                        .uri("lb://auth-service"))

                // User Service (Incluye gestión de usuarios)
                .route("user_service_route", r -> r.path("/api/v1/users/**")
                        .uri("lb://user-service"))

                // Roles y Permisos (Corrección de ruta faltante)
                .route("roles_service_route", r -> r.path("/api/v1/roles/**")
                        .uri("lb://user-service"))

                // Case Service (Gestión de casos, estados y comentarios)
                .route("case_service_route", r -> r.path("/api/v1/cases/**")
                        .uri("lb://case-service"))

                // Document Service (Alineado con el prefijo real /api/documents)
                .route("document_service_route", r -> r.path("/api/v1/documents/**")
                        .filters(f -> f.rewritePath("/api/v1/documents/(?<segment>.*)", "/api/documents/${segment}"))
                        .uri("lb://document-service"))

                // Notification Service (Alineado con el prefijo real /api/notifications)
                .route("notification_service_route", r -> r.path("/api/v1/notifications/**")
                        .filters(f -> f.rewritePath("/api/v1/notifications/(?<segment>.*)", "/api/notifications/${segment}"))
                        .uri("lb://notification-service"))
                
                .build();
    }
}

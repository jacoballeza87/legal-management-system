package com.legal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway — Punto de entrada unificado al sistema.
 *
 * Responsabilidades:
 *  - Enrutar peticiones a los microservicios correspondientes
 *  - Validar JWT en cada request entrante
 *  - Aplicar CORS global
 *  - Rate limiting por IP/usuario (Redis)
 *  - Circuit Breaker (Resilience4J)
 *  - Logging estructurado de todas las peticiones
 *
 * Puerto: 8080
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * Actuator:   http://localhost:8080/actuator/health
 *
 * Rutas principales:
 *  /api/v1/auth/**       → auth-service:8081
 *  /api/v1/users/**      → user-service:8082
 *  /api/v1/cases/**      → case-service:8083
 *  /api/v1/notifications/** → notification-service:8084
 *  /api/v1/documents/**  → document-service:8085
 */
@SpringBootApplication

public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

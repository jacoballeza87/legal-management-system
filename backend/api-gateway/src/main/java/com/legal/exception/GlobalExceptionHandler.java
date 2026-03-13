package com.legal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Manejador global de excepciones para el API Gateway.
 *
 * Transforma todas las excepciones no controladas en respuestas JSON
 * consistentes con el formato de errores del resto del sistema:
 *
 * {
 *   "status": 503,
 *   "error": "Service Unavailable",
 *   "message": "El servicio no está disponible temporalmente",
 *   "path": "/api/v1/cases/123",
 *   "timestamp": "2026-02-26T09:00:00Z"
 * }
 *
 * Casos manejados:
 *  - NotFoundException: servicio no encontrado en Eureka (503)
 *  - ResponseStatusException: errores HTTP estándar
 *  - Exception genérica: 500 interno
 *  - Fallback endpoints: respuesta estática cuando el CB está abierto
 */
@Configuration
@Order(-1) // Prioridad máxima sobre el handler por defecto de Spring
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String message;

        // ── Clasificar el error ───────────────────────────────────────────────
        if (ex instanceof NotFoundException) {
            // Microservicio no disponible en Eureka
            status  = HttpStatus.SERVICE_UNAVAILABLE;
            message = "El servicio no está disponible temporalmente. Intenta más tarde.";
            log.error("Servicio no encontrado: {}", ex.getMessage());

        } else if (ex instanceof ResponseStatusException rse) {
            status  = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : ex.getMessage();
            log.warn("ResponseStatusException: {} - {}", status, message);

        } else if (ex instanceof WebClientResponseException wcre) {
            status  = HttpStatus.valueOf(wcre.getStatusCode().value());
            message = "Error en comunicación con el servicio";
            log.error("WebClientResponseException: {} - {}", status, wcre.getMessage());

        } else if (ex instanceof io.jsonwebtoken.ExpiredJwtException) {
            status  = HttpStatus.UNAUTHORIZED;
            message = "Token expirado";
            log.warn("Token JWT expirado");

        } else if (ex instanceof io.jsonwebtoken.JwtException) {
            status  = HttpStatus.UNAUTHORIZED;
            message = "Token inválido";
            log.warn("Token JWT inválido: {}", ex.getMessage());

        } else {
            // Error genérico
            status  = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno del servidor";
            log.error("Error no controlado en Gateway: {}", ex.getMessage(), ex);
        }

        // ── Construir respuesta JSON ───────────────────────────────────────────
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String body = buildErrorBody(status, message, path);

        var buffer = exchange.getResponse().bufferFactory()
            .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String buildErrorBody(HttpStatus status, String message, String path) {
        return String.format(
            """
            {
              "status": %d,
              "error": "%s",
              "message": "%s",
              "path": "%s",
              "timestamp": "%s"
            }
            """,
            status.value(),
            status.getReasonPhrase(),
            message.replace("\"", "'"),
            path,
            Instant.now().toString()
        );
    }
}

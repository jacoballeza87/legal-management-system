package com.legal.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtro de logging estructurado.
 *
 * Registra ENTRADA y SALIDA de cada request con:
 *  - Request ID único (UUID) para correlación en logs distribuidos
 *  - Método HTTP + path + IP origen
 *  - Status code de respuesta
 *  - Tiempo total de respuesta (ms)
 *  - User ID (si está autenticado)
 *
 * El X-Request-Id se incluye en la respuesta para que el frontend
 * pueda incluirlo en reportes de error.
 *
 * Ejemplo de log:
 *  → IN  [req-abc123] POST /api/v1/auth/login from 192.168.1.1
 *  ← OUT [req-abc123] POST /api/v1/auth/login 200 OK in 145ms
 */
@Component
@Slf4j
@Order(-100) // Ejecutar antes que cualquier otro filtro
public class LoggingFilter implements WebFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String START_TIME_ATTR   = "GATEWAY_REQUEST_START";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest  request  = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // ── Generar Request ID ────────────────────────────────────────────────
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = "gw-" + UUID.randomUUID().toString().substring(0, 8);
        }
        final String finalRequestId = requestId;

        // ── Timestamp de inicio ───────────────────────────────────────────────
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        // ── Extraer IP real (detrás de proxy) ─────────────────────────────────
        String clientIp = getClientIp(request);

        // ── Log de entrada ────────────────────────────────────────────────────
        log.info("→ IN  [{}] {} {} from {}",
            finalRequestId,
            request.getMethod(),
            request.getURI().getPath(),
            clientIp
        );

        // ── Agregar Request ID a headers de respuesta ─────────────────────────
        response.getHeaders().add(REQUEST_ID_HEADER, finalRequestId);

        // ── Mutar request para propagar el Request ID a microservicios ─────────
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(REQUEST_ID_HEADER, finalRequestId)
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .doFinally(signal -> {
                long startTime = exchange.getAttribute(START_TIME_ATTR) != null
                    ? (Long) exchange.getAttribute(START_TIME_ATTR)
                    : System.currentTimeMillis();

                long duration = System.currentTimeMillis() - startTime;
                int  status   = response.getStatusCode() != null
                    ? response.getStatusCode().value()
                    : 0;

                // Log de salida
                if (status >= 500) {
                    log.error("← OUT [{}] {} {} {} in {}ms",
                        finalRequestId,
                        request.getMethod(),
                        request.getURI().getPath(),
                        status,
                        duration
                    );
                } else if (status >= 400) {
                    log.warn("← OUT [{}] {} {} {} in {}ms",
                        finalRequestId,
                        request.getMethod(),
                        request.getURI().getPath(),
                        status,
                        duration
                    );
                } else {
                    log.info("← OUT [{}] {} {} {} in {}ms",
                        finalRequestId,
                        request.getMethod(),
                        request.getURI().getPath(),
                        status,
                        duration
                    );
                }

                // Alertar si el request tardó más de 3 segundos
                if (duration > 3000) {
                    log.warn("⚠ SLOW REQUEST [{}] {} {} took {}ms",
                        finalRequestId,
                        request.getMethod(),
                        request.getURI().getPath(),
                        duration
                    );
                }
            });
    }

    private String getClientIp(ServerHttpRequest request) {
        // Verificar headers de proxies reversos
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim(); // Primer IP de la cadena
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
}

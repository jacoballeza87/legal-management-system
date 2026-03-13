package com.legal.config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Configuración de rutas del API Gateway.
 *
 * Usa "lb://" (load-balanced) para que Eureka resuelva
 * automáticamente las instancias de cada microservicio.
 *
 * Cada ruta tiene:
 *  - Path predicate
 *  - StripPrefix para quitar /api/v1 antes de reenviar
 *  - Circuit Breaker con fallback
 *  - Rate limiter por IP
 *  - Retry automático en errores 5xx
 */
@Configuration
public class GatewayConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ── Key Resolver: limitar por IP del cliente ──────────────────────────────
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var ip = exchange.getRequest().getRemoteAddress();
            return Mono.just(ip != null ? ip.getAddress().getHostAddress() : "unknown");
        };
    }

    // ── Key Resolver: limitar por usuario autenticado (JWT sub) ──────────────
    @Bean
    
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                // El subject del JWT se usa como clave (extraído por AuthenticationFilter)
                String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                return Mono.just(userId != null ? userId : "anonymous");
            }
            return Mono.just("anonymous");
        };
    }

    // ── Rate Limiter: 20 req/seg por defecto ──────────────────────────────────
  @Bean
@Primary

public RedisRateLimiter defaultRateLimiter() {
    return new RedisRateLimiter(20, 40);
}
    // ── Rate Limiter: más restrictivo para auth ───────────────────────────────
 //  @Bean
//@Qualifier("authRateLimiter")
//public RedisRateLimiter authRateLimiter() {
 //   return new RedisRateLimiter(5, 10);
//}

    // ── RUTAS ─────────────────────────────────────────────────────────────────

  @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                           @Qualifier("defaultRateLimiter") RedisRateLimiter defaultRateLimiter) {
        return builder.routes()

            // ── AUTH SERVICE ─────────────────────────────────────────────────
            .route("auth-service", r -> r
    .path("/api/v1/auth/**")
    .filters(f -> f
        .stripPrefix(2)
        .addRequestHeader("X-Gateway-Source", "legal-gateway")
        .requestRateLimiter(c -> c
            .setRateLimiter(new RedisRateLimiter(5, 10))  // inline, sin bean
            .setKeyResolver(ipKeyResolver()))
                    .circuitBreaker(c -> c
                        .setName("auth-cb")
                        .setFallbackUri("forward:/fallback/auth"))
                    .retry(config -> config
                        .setRetries(2)
                        .setStatuses(
                            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                            org.springframework.http.HttpStatus.GATEWAY_TIMEOUT)
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(500), 2, true))
                )
                .uri("lb://auth-service")
            )

            // ── USER SERVICE ─────────────────────────────────────────────────
            .route("user-service", r -> r
                .path("/api/v1/users/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Source", "legal-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(defaultRateLimiter)
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(c -> c
                        .setName("user-cb")
                        .setFallbackUri("forward:/fallback/user"))
                )
                .uri("lb://user-service")
            )

            // ── CASE SERVICE ─────────────────────────────────────────────────
            .route("case-service", r -> r
                .path("/api/v1/cases/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Source", "legal-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(defaultRateLimiter)
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(c -> c
                        .setName("case-cb")
                        .setFallbackUri("forward:/fallback/case"))
                    .retry(config -> config
                        .setRetries(1)
                        .setMethods(
                            org.springframework.http.HttpMethod.GET)
                        .setStatuses(
                            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                )
                .uri("lb://case-service")
            )

            // ── NOTIFICATION SERVICE ─────────────────────────────────────────
            .route("notification-service", r -> r
                .path("/api/v1/notifications/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Source", "legal-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(defaultRateLimiter)
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(c -> c
                        .setName("notification-cb")
                        .setFallbackUri("forward:/fallback/notification"))
                )
                .uri("lb://notification-service")
            )

            // ── DOCUMENT SERVICE ─────────────────────────────────────────────
            .route("document-service", r -> r
                .path("/api/v1/documents/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Source", "legal-gateway")
                    // El document-service puede recibir archivos grandes, rate limiter más permisivo
                    .requestRateLimiter(c -> c
                        .setRateLimiter(defaultRateLimiter)
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(c -> c
                        .setName("document-cb")
                        .setFallbackUri("forward:/fallback/document"))
                )
                .uri("lb://document-service")
            )

            // ── SWAGGER AGGREGATION ──────────────────────────────────────────
            // Permite acceder a la doc de cada servicio desde el gateway
            .route("auth-openapi", r -> r
                .path("/v3/api-docs/auth")
                .filters(f -> f.rewritePath("/v3/api-docs/auth", "/v3/api-docs"))
                .uri("lb://auth-service"))
            .route("user-openapi", r -> r
                .path("/v3/api-docs/user")
                .filters(f -> f.rewritePath("/v3/api-docs/user", "/v3/api-docs"))
                .uri("lb://user-service"))
            .route("case-openapi", r -> r
                .path("/v3/api-docs/case")
                .filters(f -> f.rewritePath("/v3/api-docs/case", "/v3/api-docs"))
                .uri("lb://case-service"))
            .route("notification-openapi", r -> r
                .path("/v3/api-docs/notification")
                .filters(f -> f.rewritePath("/v3/api-docs/notification", "/v3/api-docs"))
                .uri("lb://notification-service"))
            .route("document-openapi", r -> r
                .path("/v3/api-docs/document")
                .filters(f -> f.rewritePath("/v3/api-docs/document", "/v3/api-docs"))
                .uri("lb://document-service"))

            .build();
    }
}

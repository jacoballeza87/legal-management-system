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
 * SIN Eureka — todas las URIs apuntan directamente
 * a los nombres de los contenedores Docker.
 *
 * Puertos de cada servicio:
 *   auth-service        → 8081
 *   user-service        → 8082
 *   case-service        → 8083
 *   notification-service→ 8084
 *   document-service    → 8085
 *
 * NOTA sobre stripPrefix: los controladores de auth-service, user-service
 * y case-service están mapeados con @RequestMapping("/api/v1/...") — la
 * ruta completa, no solo el sufijo. Por eso esas tres rutas NO usan
 * stripPrefix(2): el gateway reenvía el path tal cual. notification-service
 * y document-service sí lo mantienen por ahora; confirmar su
 * @RequestMapping real antes de desplegarlos.
 */
@Configuration
public class GatewayConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // URLs de cada servicio. Default = URLs reales de Cloud Run (jacob-504115),
    // pero sobreescribibles vía env var AUTH_SERVICE_URL, USER_SERVICE_URL, etc.
    // sin necesidad de tocar este código de nuevo si las URLs cambian.
    @Value("${AUTH_SERVICE_URL:https://auth-service-308390111901.us-central1.run.app}")
    private String authServiceUrl;

    @Value("${USER_SERVICE_URL:https://user-service-308390111901.us-central1.run.app}")
    private String userServiceUrl;

    @Value("${CASE_SERVICE_URL:https://case-service-308390111901.us-central1.run.app}")
    private String caseServiceUrl;

    @Value("${NOTIFICATION_SERVICE_URL:http://notification-service:8084}")
    private String notificationServiceUrl;

    @Value("${DOCUMENT_SERVICE_URL:http://document-service:8085}")
    private String documentServiceUrl;

    // ── Key Resolver: limitar por IP del cliente ──────────────────────────────
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var ip = exchange.getRequest().getRemoteAddress();
            return Mono.just(ip != null ? ip.getAddress().getHostAddress() : "unknown");
        };
    }

    // ── Key Resolver: limitar por usuario autenticado ────────────────────────
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                return Mono.just(userId != null ? userId : "anonymous");
            }
            return Mono.just("anonymous");
        };
    }

    // ── Rate Limiter: 20 req/seg por defecto ──────────────────────────────────
    // ÚNICO RedisRateLimiter de toda la app — construido por Spring (inyecta
    // ReactiveStringRedisTemplate + RedisScript internamente). Todas las rutas
    // lo reutilizan; NUNCA instanciar RedisRateLimiter con "new" fuera de un
    // @Bean gestionado por Spring, esa variante no queda inicializada y truena
    // en tiempo de ejecución con IllegalStateException dentro de isAllowed().
    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(20, 40);
    }

    // ── RUTAS ─────────────────────────────────────────────────────────────────
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Qualifier("defaultRateLimiter") RedisRateLimiter defaultRateLimiter) {
        return builder.routes()

            // ── AUTH SERVICE ─────────────────────────────────────────────────
            // Sin stripPrefix: AuthController está mapeado en /api/v1/auth
            .route("auth-service", r -> r
                .path("/api/v1/auth/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Source", "legal-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(defaultRateLimiter)
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
                .uri(authServiceUrl)
            )

            // ── USER SERVICE ─────────────────────────────────────────────────
            // Sin stripPrefix: UserController está mapeado en /api/v1/users
            .route("user-service", r -> r
                .path("/api/v1/users/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Source", "legal-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(defaultRateLimiter)
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(c -> c
                        .setName("user-cb")
                        .setFallbackUri("forward:/fallback/user"))
                )
                .uri(userServiceUrl)
            )

            // ── CASE SERVICE ─────────────────────────────────────────────────
            // Sin stripPrefix: CaseController está mapeado en /api/v1/cases
            .route("case-service", r -> r
                .path("/api/v1/cases/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway-Source", "legal-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(defaultRateLimiter)
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(c -> c
                        .setName("case-cb")
                        .setFallbackUri("forward:/fallback/case"))
                    .retry(config -> config
                        .setRetries(1)
                        .setMethods(org.springframework.http.HttpMethod.GET)
                        .setStatuses(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                )
                // ✅ CORREGIDO: http directo en lugar de lb://
                .uri(caseServiceUrl)
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
                // ✅ CORREGIDO: http directo en lugar de lb://
                .uri(notificationServiceUrl)
            )

            // ── DOCUMENT SERVICE ─────────────────────────────────────────────
            .route("document-service", r -> r
                .path("/api/v1/documents/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Gateway-Source", "legal-gateway")
                    .requestRateLimiter(c -> c
                        .setRateLimiter(defaultRateLimiter)
                        .setKeyResolver(userKeyResolver()))
                    .circuitBreaker(c -> c
                        .setName("document-cb")
                        .setFallbackUri("forward:/fallback/document"))
                )
                // ✅ CORREGIDO: http directo en lugar de lb://
                .uri(documentServiceUrl)
            )

            // ── SWAGGER AGGREGATION ──────────────────────────────────────────
            .route("auth-openapi", r -> r
                .path("/v3/api-docs/auth")
                .filters(f -> f.rewritePath("/v3/api-docs/auth", "/v3/api-docs"))
                .uri(authServiceUrl))
            .route("user-openapi", r -> r
                .path("/v3/api-docs/user")
                .filters(f -> f.rewritePath("/v3/api-docs/user", "/v3/api-docs"))
                .uri(userServiceUrl))
            .route("case-openapi", r -> r
                .path("/v3/api-docs/case")
                .filters(f -> f.rewritePath("/v3/api-docs/case", "/v3/api-docs"))
                .uri(caseServiceUrl))
            .route("notification-openapi", r -> r
                .path("/v3/api-docs/notification")
                .filters(f -> f.rewritePath("/v3/api-docs/notification", "/v3/api-docs"))
                .uri(notificationServiceUrl))
            .route("document-openapi", r -> r
                .path("/v3/api-docs/document")
                .filters(f -> f.rewritePath("/v3/api-docs/document", "/v3/api-docs"))
                .uri(documentServiceUrl))

            .build();
    }
}

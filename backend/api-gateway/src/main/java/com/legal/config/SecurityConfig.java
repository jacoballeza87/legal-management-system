package com.legal.config;

import com.legal.filter.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

/**
 * Seguridad del API Gateway.
 *
 * Estrategia:
 *  - Stateless (JWT, sin sesión HTTP)
 *  - Rutas públicas: /api/v1/auth/** sin restricción
 *  - Todas las demás rutas: requieren JWT válido
 *  - El AuthenticationFilter valida el JWT ANTES de enrutar
 *
 * El Gateway NO genera tokens — solo los valida y propaga
 * los claims (userId, role) como headers internos al microservicio.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final AuthenticationFilter authenticationFilter;

    public SecurityConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            // ── Deshabilitar protecciones no necesarias en API REST ─────────
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)

            // ── Sin sesión HTTP (JWT stateless) ─────────────────────────────
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

            // ── Control de acceso ────────────────────────────────────────────
            .authorizeExchange(exchanges -> exchanges
                // Rutas completamente públicas
                .pathMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/forgot-password",
                    "/api/v1/auth/reset-password",
                    "/api/v1/auth/oauth2/**",
                    "/api/v1/auth/refresh"
                ).permitAll()

                // Actuator y documentación (solo internas o con IP whitelist)
                .pathMatchers(
                    "/actuator/**",
                    "/fallback/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()

                // Todo lo demás requiere JWT válido
                .anyExchange().authenticated()
            )

            // ── Errores de autenticación ─────────────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((exchange, e) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                    var body = """
                        {"status":401,"error":"Unauthorized","message":"Token requerido o inválido"}
                        """.getBytes();
                    var buffer = exchange.getResponse().bufferFactory().wrap(body);
                    return exchange.getResponse().writeWith(Mono.just(buffer));
                })
                .accessDeniedHandler((exchange, e) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                    var body = """
                        {"status":403,"error":"Forbidden","message":"Acceso denegado"}
                        """.getBytes();
                    var buffer = exchange.getResponse().bufferFactory().wrap(body);
                    return exchange.getResponse().writeWith(Mono.just(buffer));
                })
            )

            // ── Filtro JWT personalizado ─────────────────────────────────────
            .addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

            .build();
    }
}

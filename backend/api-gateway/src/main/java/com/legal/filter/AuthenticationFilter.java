package com.legal.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class AuthenticationFilter implements WebFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password",
        "/api/v1/auth/refresh"
    );

    private static final List<String> PUBLIC_PREFIXES = Arrays.asList(
        "/api/v1/auth/oauth2/",
        "/actuator/",
        "/fallback/",
        "/swagger-ui",
        "/v3/api-docs",
        "/webjars/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Request sin token: {} {}", exchange.getRequest().getMethod(), path);
            return unauthorized(exchange, "Token de autenticación requerido");
        }

        String token = authHeader.substring(7);

        Claims claims;
        try {
            claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expirado para path: {}", path);
            return unauthorized(exchange, "Token expirado");
        } catch (SignatureException e) {
            log.warn("Firma JWT inválida para path: {}", path);
            return unauthorized(exchange, "Token inválido");
        } catch (MalformedJwtException e) {
            log.warn("Token malformado para path: {}", path);
            return unauthorized(exchange, "Token malformado");
        } catch (Exception e) {
            log.error("Error inesperado validando JWT: {}", e.getMessage());
            return unauthorized(exchange, "Error de autenticación");
        }

        String userId   = claims.getSubject();
        String username = claims.get("username", String.class);
        String role     = claims.get("role",     String.class);
        String email    = claims.get("email",    String.class);
        String name     = claims.get("name",     String.class);

        log.debug("JWT válido → userId={}, role={}, path={}", userId, role, path);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .header("X-User-Id",       userId   != null ? userId   : "")
            .header("X-User-Name",     username != null ? username : "")
            .header("X-User-Role",     role     != null ? role     : "")
            .header("X-User-Email",    email    != null ? email    : "")
            .header("X-User-FullName", name     != null ? name     : "")
            .header("X-Authenticated", "true")
            .build();

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")));
        var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) return true;
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = String.format(
            "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
            message,
            exchange.getRequest().getURI().getPath()
        );
        var buffer = exchange.getResponse().bufferFactory()
            .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
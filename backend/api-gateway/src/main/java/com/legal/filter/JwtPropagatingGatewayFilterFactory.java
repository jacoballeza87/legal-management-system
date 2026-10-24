package com.legal.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class JwtPropagatingGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtPropagatingGatewayFilterFactory.Config> {

    public JwtPropagatingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Extraer el header de autorización de la petición entrante del cliente
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && !authHeader.isEmpty()) {
                // Mutar la petición saliente hacia el microservicio backend para asegurar que lleve el JWT
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, authHeader)))
                        .build();
                return chain.filter(mutatedExchange);
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Configuraciones adicionales si se requieren a futuro
    }
}

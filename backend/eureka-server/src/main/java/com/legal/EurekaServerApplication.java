package com.legal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor de Service Discovery (Netflix Eureka).
 *
 * Todos los microservicios se registran aquí al iniciarse.
 * El API Gateway consulta Eureka para resolver las rutas.
 *
 * Puerto: 8761
 * Dashboard: http://eureka-server:8761
 * API REST: http://eureka-server:8761/eureka/apps
 *
 * Orden de inicio recomendado:
 *  1. eureka-server  (este)
 *  2. config-server
 *  3. api-gateway
 *  4. auth-service, user-service, case-service, etc.
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}

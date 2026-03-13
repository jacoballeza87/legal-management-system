package com.legal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;


/**
 * Servidor de Configuración Centralizada.
 *
 * Sirve configuración a todos los microservicios desde:
 *  - Repositorio Git (producción): rama main, directorio /config
 *  - Classpath local (desarrollo): src/main/resources/config/
 *
 * Puerto: 8888
 * Health: http://localhost:8888/actuator/health
 * Ejemplo: GET http://localhost:8888/auth-service/default
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}

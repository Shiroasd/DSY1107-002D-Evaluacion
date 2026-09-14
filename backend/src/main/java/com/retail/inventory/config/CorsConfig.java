package com.retail.inventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración explícita de CORS a nivel de Spring Web MVC.
 * Habilita peticiones cruzadas originadas desde la dirección IP pública del Frontend desplegado en AWS:
 * https://32.193.45.223/
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://32.193.45.223",
                        "http://localhost:4200",
                        "http://127.0.0.1:4200",
                        "https://localhost:4200"
                )
                .allowedOriginPatterns(
                        "https://32.193.45.223*",
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With",
                        "Access-Control-Request-Method",
                        "Access-Control-Request-Headers"
                )
                .exposedHeaders("Authorization", "Location", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

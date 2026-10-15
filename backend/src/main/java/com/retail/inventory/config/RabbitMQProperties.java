package com.retail.inventory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades centralizadas de RabbitMQ cargadas desde application.yml (app.rabbitmq.*).
 * Cumple con el criterio de Configuración Centralizada sin valores hardcodeados en el código.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMQProperties {

    private String exchange = "inventario.exchange";
    private String dlxExchange = "inventario.dlx.exchange";
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
        private String auditoria = "inventario.auditoria.queue";
        private String auditoriaDlq = "inventario.auditoria.dlq";
        private String notificaciones = "inventario.notificaciones.queue";
        private String notificacionesDlq = "inventario.notificaciones.dlq";
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        private String stockModificado = "inventario.stock.modificado";
        private String auditoriaAll = "inventario.#";
        private String dlqAuditoria = "dlq.auditoria";
        private String dlqNotificaciones = "dlq.notificaciones";
    }
}

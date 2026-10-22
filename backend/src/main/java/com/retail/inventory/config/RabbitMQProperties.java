package com.retail.inventory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades centralizadas de RabbitMQ (app.rabbitmq.* en application.yml).
 * Única fuente de nombres de exchanges, colas y routing keys: ninguna clase del sistema
 * declara estos valores de forma literal.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMQProperties {

    private String exchange;
    private String dlxExchange;
    private int stockCriticoUmbral = 3;
    private Queues queues = new Queues();
    private Exchanges exchanges = new Exchanges();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
        // Topología EP3 / microservicios de inventario
        private String auditoria;
        private String auditoriaDlq;
        private String notificaciones;
        private String notificacionesDlq;
        private String reposicion;
        private String reposicionDlq;

        // Topología complementaria de alertas
        private String stockAlert = "stock-alert";
        private String auditLog = "audit-log";
        private String deadLetter = "dead-letter";
    }

    @Getter
    @Setter
    public static class Exchanges {
        private String inventoryTopic = "inventory.topic.exchange";
        private String inventoryDlx = "inventory.dlx.exchange";
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        // Topología EP3 / microservicios de inventario
        private String stockModificado;
        private String stockCritico;
        private String auditoriaAll;
        private String dlqAuditoria;
        private String dlqNotificaciones;
        private String dlqReposicion;

        // Topología complementaria de alertas
        private String productStockLow = "product.stock.low";
        private String productAll = "product.#";
        private String inventoryDlq = "inventory.dlq";
    }
}

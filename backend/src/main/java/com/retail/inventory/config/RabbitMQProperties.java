package com.retail.inventory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades centralizadas de configuración de RabbitMQ.
 * Mapeadas con prefijo 'app.rabbitmq' para evitar constantes literales en código de negocio.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMQProperties {

    private Queues queues = new Queues();
    private Exchanges exchanges = new Exchanges();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
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
        private String productStockLow = "product.stock.low";
        private String productAll = "product.#";
        private String inventoryDlq = "inventory.dlq";
    }
}

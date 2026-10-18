package com.retail.inventory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
<<<<<<< HEAD
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
=======
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
    private int stockCriticoUmbral;
    private Queues queues = new Queues();
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
<<<<<<< HEAD
        private String stockAlert = "stock-alert";
        private String auditLog = "audit-log";
        private String deadLetter = "dead-letter";
    }

    @Getter
    @Setter
    public static class Exchanges {
        private String inventoryTopic = "inventory.topic.exchange";
        private String inventoryDlx = "inventory.dlx.exchange";
=======
        private String auditoria;
        private String auditoriaDlq;
        private String notificaciones;
        private String notificacionesDlq;
        private String reposicion;
        private String reposicionDlq;
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
    }

    @Getter
    @Setter
    public static class RoutingKeys {
<<<<<<< HEAD
        private String productStockLow = "product.stock.low";
        private String productAll = "product.#";
        private String inventoryDlq = "inventory.dlq";
=======
        private String stockModificado;
        private String stockCritico;
        private String auditoriaAll;
        private String dlqAuditoria;
        private String dlqNotificaciones;
        private String dlqReposicion;
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
    }
}

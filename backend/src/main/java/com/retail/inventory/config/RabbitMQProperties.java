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
    private int stockCriticoUmbral;
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
        private String auditoria;
        private String auditoriaDlq;
        private String notificaciones;
        private String notificacionesDlq;
        private String reposicion;
        private String reposicionDlq;
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        private String stockModificado;
        private String stockCritico;
        private String auditoriaAll;
        private String dlqAuditoria;
        private String dlqNotificaciones;
        private String dlqReposicion;
    }
}

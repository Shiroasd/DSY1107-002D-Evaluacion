package com.retail.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "inventario.exchange";
    public static final String QUEUE_AUDITORIA = "inventario.auditoria.queue";
    public static final String QUEUE_NOTIFICACIONES = "inventario.notificaciones.queue";

    // Creamos el Exchange de tipo Topic
    @Bean
    public TopicExchange inventarioExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // Creamos las dos colas
    @Bean
    public Queue auditoriaQueue() {
        return new Queue(QUEUE_AUDITORIA, true);
    }

    @Bean
    public Queue notificacionesQueue() {
        return new Queue(QUEUE_NOTIFICACIONES, true);
    }

    // Enlazamos la cola de auditoría para que escuche cualquier evento de inventario ("inventario.#")
    @Bean
    public Binding bindingAuditoria(Queue auditoriaQueue, TopicExchange inventarioExchange) {
        return BindingBuilder.bind(auditoriaQueue).to(inventarioExchange).with("inventario.#");
    }

    // Enlazamos la cola de notificaciones para que escuche solo modificaciones de stock
    @Bean
    public Binding bindingNotificaciones(Queue notificacionesQueue, TopicExchange inventarioExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(inventarioExchange).with("inventario.stock.modificado");
    }
}
package com.retail.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración centralizada de RabbitMQ para el sistema de inventario Cloud Native (Pauta EP3).
 * Declara:
 * 1. TopicExchange principal para eventos de negocio (inventoryExchange).
 * 2. DirectExchange para Dead Letter Exchanges (deadLetterExchange).
 * 3. Colas principales configuradas con Map de argumentos x-dead-letter-exchange y x-dead-letter-routing-key.
 * 4. Colas Dead Letter Queues (DLQ) para captura de mensajes con error de procesamiento.
 * 5. Bindings para conectar colas principales y DLQs a sus exchanges correspondientes.
 * 6. MessageConverter JSON (Jackson) y contenedor con confirmación manual (AcknowledgeMode.MANUAL).
 * 7. Bean AmqpAdmin para gestión dinámica de topologías.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:inventario.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.dlx-exchange:inventario.dlx.exchange}")
    private String dlxExchangeName;

    @Value("${app.rabbitmq.queues.auditoria:inventario.auditoria.queue}")
    private String auditoriaQueueName;

    @Value("${app.rabbitmq.queues.auditoria-dlq:inventario.auditoria.dlq}")
    private String auditoriaDlqQueueName;

    @Value("${app.rabbitmq.queues.notificaciones:inventario.notificaciones.queue}")
    private String notificacionesQueueName;

    @Value("${app.rabbitmq.queues.notificaciones-dlq:inventario.notificaciones.dlq}")
    private String notificacionesDlqQueueName;

    @Value("${app.rabbitmq.routing-keys.stock-modificado:inventario.stock.modificado}")
    private String stockModificadoRoutingKey;

    @Value("${app.rabbitmq.routing-keys.auditoria-all:inventario.#}")
    private String auditoriaAllRoutingKey;

    @Value("${app.rabbitmq.routing-keys.dlq-auditoria:dlq.auditoria}")
    private String dlqAuditoriaRoutingKey;

    @Value("${app.rabbitmq.routing-keys.dlq-notificaciones:dlq.notificaciones}")
    private String dlqNotificacionesRoutingKey;

    /**
     * Constructor público sin argumentos simple.
     * Cumple con la compatibilidad Java 17 / Java 21 evitando sintaxis experimental en clases @Configuration.
     */
    public RabbitMQConfig() {
    }

    // ==========================================
    // EXCHANGES
    // ==========================================

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxExchangeName, true, false);
    }

    // ==========================================
    // DEAD LETTER QUEUES (DLQs)
    // ==========================================

    @Bean
    public Queue auditoriaDlqQueue() {
        return QueueBuilder.durable(auditoriaDlqQueueName).build();
    }

    @Bean
    public Queue notificacionesDlqQueue() {
        return QueueBuilder.durable(notificacionesDlqQueueName).build();
    }

    // ==========================================
    // QUEUES PRINCIPALES (Enlazadas a DLX vía Map)
    // ==========================================

    @Bean
    public Queue auditoriaQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlxExchangeName);
        args.put("x-dead-letter-routing-key", dlqAuditoriaRoutingKey);
        return QueueBuilder.durable(auditoriaQueueName)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue notificacionesQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlxExchangeName);
        args.put("x-dead-letter-routing-key", dlqNotificacionesRoutingKey);
        return QueueBuilder.durable(notificacionesQueueName)
                .withArguments(args)
                .build();
    }

    // ==========================================
    // BINDINGS PRINCIPALES
    // ==========================================

    @Bean
    public Binding bindingAuditoria(Queue auditoriaQueue, TopicExchange inventoryExchange) {
        return BindingBuilder.bind(auditoriaQueue)
                .to(inventoryExchange)
                .with(auditoriaAllRoutingKey);
    }

    @Bean
    public Binding bindingNotificaciones(Queue notificacionesQueue, TopicExchange inventoryExchange) {
        return BindingBuilder.bind(notificacionesQueue)
                .to(inventoryExchange)
                .with(stockModificadoRoutingKey);
    }

    // ==========================================
    // BINDINGS DEAD LETTER (DLQ)
    // ==========================================

    @Bean
    public Binding bindingAuditoriaDlq(Queue auditoriaDlqQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(auditoriaDlqQueue)
                .to(deadLetterExchange)
                .with(dlqAuditoriaRoutingKey);
    }

    @Bean
    public Binding bindingNotificacionesDlq(Queue notificacionesDlqQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificacionesDlqQueue)
                .to(deadLetterExchange)
                .with(dlqNotificacionesRoutingKey);
    }

    // ==========================================
    // CONVERTER, TEMPLATE Y CONTAINER FACTORY
    // ==========================================

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
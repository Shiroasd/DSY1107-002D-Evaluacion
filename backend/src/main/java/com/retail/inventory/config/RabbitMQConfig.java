package com.retail.inventory.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de infraestructura AMQP / RabbitMQ.
 * Declara exchanges, colas durables con política DLX, bindings formales,
 * serializador JSON estándar y contenedor de listeners con confirmación manual.
 */
@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final RabbitMQProperties properties;

    // ==========================================
    // Serialización y Plantillas AMQP
    // ==========================================

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.listener.simple.auto-startup:true}")
    private boolean autoStartup;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    // ==========================================
    // Exchanges (Topic & Direct DLX)
    // ==========================================

    @Bean
    public TopicExchange inventoryTopicExchange() {
        return new TopicExchange(properties.getExchanges().getInventoryTopic(), true, false);
    }

    @Bean
    public DirectExchange inventoryDlxExchange() {
        return new DirectExchange(properties.getExchanges().getInventoryDlx(), true, false);
    }

    // ==========================================
    // Colas Durables con DLQ configurada
    // ==========================================

    @Bean
    public Queue stockAlertQueue() {
        return QueueBuilder.durable(properties.getQueues().getStockAlert())
                .withArgument("x-dead-letter-exchange", properties.getExchanges().getInventoryDlx())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeys().getInventoryDlq())
                .build();
    }

    @Bean
    public Queue auditLogQueue() {
        return QueueBuilder.durable(properties.getQueues().getAuditLog())
                .withArgument("x-dead-letter-exchange", properties.getExchanges().getInventoryDlx())
                .withArgument("x-dead-letter-routing-key", properties.getRoutingKeys().getInventoryDlq())
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(properties.getQueues().getDeadLetter()).build();
    }

    // ==========================================
    // Bindings Formales
    // ==========================================

    @Bean
    public Binding stockAlertBinding(Queue stockAlertQueue, TopicExchange inventoryTopicExchange) {
        return BindingBuilder.bind(stockAlertQueue)
                .to(inventoryTopicExchange)
                .with(properties.getRoutingKeys().getProductStockLow());
    }

    @Bean
    public Binding auditLogBinding(Queue auditLogQueue, TopicExchange inventoryTopicExchange) {
        return BindingBuilder.bind(auditLogQueue)
                .to(inventoryTopicExchange)
                .with(properties.getRoutingKeys().getProductAll());
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange inventoryDlxExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(inventoryDlxExchange)
                .with(properties.getRoutingKeys().getInventoryDlq());
    }
}

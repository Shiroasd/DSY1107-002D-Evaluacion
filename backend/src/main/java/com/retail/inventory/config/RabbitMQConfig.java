package com.retail.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 * Topología RabbitMQ del sistema de inventario. Todos los nombres provienen de {@link RabbitMQProperties}.
 *
 * Rutas de mensajería (casos de uso):
 *  1. Auditoría:      inventario.exchange (topic) --[inventario.#]--------------> cola auditoría      --(DLX)--> DLQ auditoría
 *  2. Notificaciones: inventario.exchange (topic) --[inventario.stock.modificado]-> cola notificaciones --(DLX)--> DLQ notificaciones
 *  3. Reposición:     inventario.exchange (topic) --[reposicion.stock.critico]---> cola reposición     --(DLX)--> DLQ reposición
 * Las DLQ se enlazan al exchange direct de dead letters (DLX).
 */
@Configuration
public class RabbitMQConfig {

    private static final String DLX_ARG = "x-dead-letter-exchange";
    private static final String DLX_RK_ARG = "x-dead-letter-routing-key";

    private final RabbitMQProperties props;

    public RabbitMQConfig(RabbitMQProperties props) {
        this.props = props;
    }

    // ---------- Exchanges ----------

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(props.getExchange(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.getDlxExchange(), true, false);
    }

    // ---------- Colas principales (con dead-lettering hacia el DLX) ----------

    @Bean
    public Queue auditoriaQueue() {
        return mainQueue(props.getQueues().getAuditoria(), props.getRoutingKeys().getDlqAuditoria());
    }

    @Bean
    public Queue notificacionesQueue() {
        return mainQueue(props.getQueues().getNotificaciones(), props.getRoutingKeys().getDlqNotificaciones());
    }

    @Bean
    public Queue reposicionQueue() {
        return mainQueue(props.getQueues().getReposicion(), props.getRoutingKeys().getDlqReposicion());
    }

    // ---------- Dead Letter Queues ----------

    @Bean
    public Queue auditoriaDlqQueue() {
        return QueueBuilder.durable(props.getQueues().getAuditoriaDlq()).build();
    }

    @Bean
    public Queue notificacionesDlqQueue() {
        return QueueBuilder.durable(props.getQueues().getNotificacionesDlq()).build();
    }

    @Bean
    public Queue reposicionDlqQueue() {
        return QueueBuilder.durable(props.getQueues().getReposicionDlq()).build();
    }

    // ---------- Bindings principales ----------

    @Bean
    public Binding bindingAuditoria() {
        return BindingBuilder.bind(auditoriaQueue()).to(inventoryExchange())
                .with(props.getRoutingKeys().getAuditoriaAll());
    }

    @Bean
    public Binding bindingNotificaciones() {
        return BindingBuilder.bind(notificacionesQueue()).to(inventoryExchange())
                .with(props.getRoutingKeys().getStockModificado());
    }

    @Bean
    public Binding bindingReposicion() {
        return BindingBuilder.bind(reposicionQueue()).to(inventoryExchange())
                .with(props.getRoutingKeys().getStockCritico());
    }

    // ---------- Bindings de DLQ ----------

    @Bean
    public Binding bindingAuditoriaDlq() {
        return BindingBuilder.bind(auditoriaDlqQueue()).to(deadLetterExchange())
                .with(props.getRoutingKeys().getDlqAuditoria());
    }

    @Bean
    public Binding bindingNotificacionesDlq() {
        return BindingBuilder.bind(notificacionesDlqQueue()).to(deadLetterExchange())
                .with(props.getRoutingKeys().getDlqNotificaciones());
    }

    @Bean
    public Binding bindingReposicionDlq() {
        return BindingBuilder.bind(reposicionDlqQueue()).to(deadLetterExchange())
                .with(props.getRoutingKeys().getDlqReposicion());
    }

    // ---------- Converter, template, container factory y admin ----------

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
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
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

    private Queue mainQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .withArgument(DLX_ARG, props.getDlxExchange())
                .withArgument(DLX_RK_ARG, deadLetterRoutingKey)
                .build();
    }
}

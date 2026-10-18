package com.retail.inventory.service.impl;

import com.retail.inventory.dto.admin.CreateBindingRequestDto;
import com.retail.inventory.dto.admin.CreateExchangeRequestDto;
import com.retail.inventory.dto.admin.CreateQueueRequestDto;
import com.retail.inventory.exception.BadRequestException;
import com.retail.inventory.exception.ResourceNotFoundException;
import com.retail.inventory.service.RabbitAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implementación de administración de topología RabbitMQ encapsulando AmqpAdmin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminServiceImpl implements RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    @Override
    public String createQueue(CreateQueueRequestDto dto) {
        String queueName = dto.getName().trim();
        QueueBuilder builder = dto.isDurable() ? QueueBuilder.durable(queueName) : QueueBuilder.nonDurable(queueName);

        if (dto.isExclusive()) {
            builder.exclusive();
        }
        if (dto.isAutoDelete()) {
            builder.autoDelete();
        }
        if (dto.getDeadLetterExchange() != null && !dto.getDeadLetterExchange().isBlank()) {
            builder.withArgument("x-dead-letter-exchange", dto.getDeadLetterExchange().trim());
        }
        if (dto.getDeadLetterRoutingKey() != null && !dto.getDeadLetterRoutingKey().isBlank()) {
            builder.withArgument("x-dead-letter-routing-key", dto.getDeadLetterRoutingKey().trim());
        }

        Queue queue = builder.build();
        String declared = amqpAdmin.declareQueue(queue);
        log.info("Cola declarada en RabbitMQ: {}", declared != null ? declared : queueName);
        return declared != null ? declared : queueName;
    }

    @Override
    public boolean deleteQueue(String queueName) {
        log.info("Eliminando cola en RabbitMQ: {}", queueName);
        return amqpAdmin.deleteQueue(queueName.trim());
    }

    @Override
    public void purgeQueue(String queueName) {
        log.info("Purgando mensajes de la cola: {}", queueName);
        amqpAdmin.purgeQueue(queueName.trim(), false);
    }

    @Override
    public void createExchange(CreateExchangeRequestDto dto) {
        String name = dto.getName().trim();
        boolean durable = dto.isDurable();
        boolean autoDelete = dto.isAutoDelete();
        Exchange exchange;

        switch (dto.getType().trim().toLowerCase()) {
            case "direct" -> exchange = new DirectExchange(name, durable, autoDelete);
            case "topic" -> exchange = new TopicExchange(name, durable, autoDelete);
            case "fanout" -> exchange = new FanoutExchange(name, durable, autoDelete);
            case "headers" -> exchange = new HeadersExchange(name, durable, autoDelete);
            default -> throw new BadRequestException("Tipo de exchange no soportado: " + dto.getType());
        }

        amqpAdmin.declareExchange(exchange);
        log.info("Exchange '{}' de tipo '{}' declarado en RabbitMQ.", name, dto.getType());
    }

    @Override
    public boolean deleteExchange(String exchangeName) {
        log.info("Eliminando exchange en RabbitMQ: {}", exchangeName);
        return amqpAdmin.deleteExchange(exchangeName.trim());
    }

    @Override
    public void createBinding(CreateBindingRequestDto dto) {
        Binding binding = new Binding(
                dto.getQueueName().trim(),
                Binding.DestinationType.QUEUE,
                dto.getExchangeName().trim(),
                dto.getRoutingKey().trim(),
                null
        );
        amqpAdmin.declareBinding(binding);
        log.info("Binding asociado: Cola '{}' -> Exchange '{}' con RoutingKey '{}'",
                dto.getQueueName(), dto.getExchangeName(), dto.getRoutingKey());
    }

    @Override
    public void deleteBinding(CreateBindingRequestDto dto) {
        Binding binding = new Binding(
                dto.getQueueName().trim(),
                Binding.DestinationType.QUEUE,
                dto.getExchangeName().trim(),
                dto.getRoutingKey().trim(),
                null
        );
        amqpAdmin.removeBinding(binding);
        log.info("Binding desvinculado: Cola '{}' de Exchange '{}'", dto.getQueueName(), dto.getExchangeName());
    }

    @Override
    public Map<String, Object> getQueueInfo(String queueName) {
        Properties properties = amqpAdmin.getQueueProperties(queueName.trim());
        if (properties == null) {
            throw new ResourceNotFoundException("Cola no encontrada o inaccesible en el broker: " + queueName);
        }

        Map<String, Object> info = new HashMap<>();
        for (String stringPropertyName : properties.stringPropertyNames()) {
            info.put(stringPropertyName, properties.getProperty(stringPropertyName));
        }
        return info;
    }
}

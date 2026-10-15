package com.retail.inventory.service.impl;

import com.retail.inventory.dto.BindingRequestDto;
import com.retail.inventory.dto.ExchangeRequestDto;
import com.retail.inventory.dto.QueueRequestDto;
import com.retail.inventory.exception.BadRequestException;
import com.retail.inventory.service.RabbitAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementación de administración dinámica de RabbitMQ utilizando AmqpAdmin.
 */
@Service
@Slf4j
public class RabbitAdminServiceImpl implements RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    @Autowired
    public RabbitAdminServiceImpl(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    @Override
    public Map<String, Object> createQueue(QueueRequestDto requestDto) {
        log.info("[RABBIT ADMIN] Declarando cola '{}' (durable: {}, exclusive: {}, autoDelete: {})",
                requestDto.getName(), requestDto.isDurable(), requestDto.isExclusive(), requestDto.isAutoDelete());

        QueueBuilder builder = requestDto.isDurable()
                ? QueueBuilder.durable(requestDto.getName())
                : QueueBuilder.nonDurable(requestDto.getName());

        if (requestDto.isExclusive()) {
            builder.exclusive();
        }
        if (requestDto.isAutoDelete()) {
            builder.autoDelete();
        }
        if (requestDto.getDeadLetterExchange() != null && !requestDto.getDeadLetterExchange().isBlank()) {
            builder.withArgument("x-dead-letter-exchange", requestDto.getDeadLetterExchange().trim());
        }
        if (requestDto.getDeadLetterRoutingKey() != null && !requestDto.getDeadLetterRoutingKey().isBlank()) {
            builder.withArgument("x-dead-letter-routing-key", requestDto.getDeadLetterRoutingKey().trim());
        }
        if (requestDto.getArguments() != null) {
            requestDto.getArguments().forEach(builder::withArgument);
        }

        Queue queue = builder.build();
        String declaredName = amqpAdmin.declareQueue(queue);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Cola declarada exitosamente en RabbitMQ");
        response.put("queueName", declaredName != null ? declaredName : requestDto.getName());
        response.put("durable", requestDto.isDurable());
        response.put("exclusive", requestDto.isExclusive());
        response.put("autoDelete", requestDto.isAutoDelete());
        response.put("deadLetterExchange", requestDto.getDeadLetterExchange());
        response.put("deadLetterRoutingKey", requestDto.getDeadLetterRoutingKey());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @Override
    public Map<String, Object> deleteQueue(String queueName) {
        log.info("[RABBIT ADMIN] Eliminando cola '{}'", queueName);
        boolean deleted = amqpAdmin.deleteQueue(queueName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", deleted ? "SUCCESS" : "NOT_FOUND_OR_SKIPPED");
        response.put("message", deleted ? "Cola eliminada exitosamente" : "No fue posible eliminar la cola o no existía");
        response.put("queueName", queueName);
        response.put("deleted", deleted);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @Override
    public Map<String, Object> createExchange(ExchangeRequestDto requestDto) {
        String type = requestDto.getType().trim().toUpperCase();
        log.info("[RABBIT ADMIN] Declarando Exchange '{}' de tipo '{}' (durable: {})",
                requestDto.getName(), type, requestDto.isDurable());

        Exchange exchange;
        switch (type) {
            case "DIRECT":
                exchange = new DirectExchange(requestDto.getName(), requestDto.isDurable(), requestDto.isAutoDelete(), requestDto.getArguments());
                break;
            case "TOPIC":
                exchange = new TopicExchange(requestDto.getName(), requestDto.isDurable(), requestDto.isAutoDelete(), requestDto.getArguments());
                break;
            case "FANOUT":
                exchange = new FanoutExchange(requestDto.getName(), requestDto.isDurable(), requestDto.isAutoDelete(), requestDto.getArguments());
                break;
            case "HEADERS":
                exchange = new HeadersExchange(requestDto.getName(), requestDto.isDurable(), requestDto.isAutoDelete(), requestDto.getArguments());
                break;
            default:
                throw new BadRequestException("Tipo de Exchange no soportado: " + type + ". Tipos válidos: DIRECT, TOPIC, FANOUT, HEADERS.");
        }

        amqpAdmin.declareExchange(exchange);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Exchange declarado exitosamente en RabbitMQ");
        response.put("exchangeName", requestDto.getName());
        response.put("type", type);
        response.put("durable", requestDto.isDurable());
        response.put("autoDelete", requestDto.isAutoDelete());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @Override
    public Map<String, Object> deleteExchange(String exchangeName) {
        log.info("[RABBIT ADMIN] Eliminando Exchange '{}'", exchangeName);
        boolean deleted = amqpAdmin.deleteExchange(exchangeName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", deleted ? "SUCCESS" : "NOT_FOUND_OR_SKIPPED");
        response.put("message", deleted ? "Exchange eliminado exitosamente" : "No fue posible eliminar el exchange o no existía");
        response.put("exchangeName", exchangeName);
        response.put("deleted", deleted);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @Override
    public Map<String, Object> createBinding(BindingRequestDto requestDto) {
        log.info("[RABBIT ADMIN] Declarando Binding entre destino '{}' ({}) y exchange '{}' con routing key '{}'",
                requestDto.getDestination(), requestDto.getDestinationType(), requestDto.getExchange(), requestDto.getRoutingKey());

        Binding.DestinationType destType;
        try {
            destType = Binding.DestinationType.valueOf(requestDto.getDestinationType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Tipo de destino inválido: " + requestDto.getDestinationType() + ". Valores permitidos: QUEUE, EXCHANGE.");
        }

        Binding binding = new Binding(
                requestDto.getDestination(),
                destType,
                requestDto.getExchange(),
                requestDto.getRoutingKey(),
                requestDto.getArguments()
        );

        amqpAdmin.declareBinding(binding);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Binding declarado exitosamente en RabbitMQ");
        response.put("destination", requestDto.getDestination());
        response.put("destinationType", destType.name());
        response.put("exchange", requestDto.getExchange());
        response.put("routingKey", requestDto.getRoutingKey());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}

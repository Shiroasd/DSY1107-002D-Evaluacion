package com.retail.inventory.service.impl;

<<<<<<< HEAD
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
=======
import com.retail.inventory.config.RabbitMQProperties;
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
import java.util.regex.Pattern;

/**
 * Implementación de administración dinámica de RabbitMQ utilizando AmqpAdmin.
 */
@Service
@Slf4j
public class RabbitAdminServiceImpl implements RabbitAdminService {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9._-]{1,255}$");
    private static final String RESERVED_PREFIX = "amq.";

    private final AmqpAdmin amqpAdmin;
    private final RabbitMQProperties props;

    @Autowired
    public RabbitAdminServiceImpl(AmqpAdmin amqpAdmin, RabbitMQProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.props = props;
    }

    // ---------- Validaciones de negocio ----------

    private void validateName(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            throw new BadRequestException("Nombre inválido: solo se permiten alfanuméricos, puntos, guiones y guiones bajos (máx. 255).");
        }
        if (name.toLowerCase().startsWith(RESERVED_PREFIX)) {
            throw new BadRequestException("El prefijo '" + RESERVED_PREFIX + "' está reservado por RabbitMQ.");
        }
    }

    /** Impide eliminar la topología base del sistema (definida en application.yml). */
    private void assertNotSystemResource(String name) {
        RabbitMQProperties.Queues q = props.getQueues();
        if (name.equals(props.getExchange()) || name.equals(props.getDlxExchange())
                || name.equals(q.getAuditoria()) || name.equals(q.getAuditoriaDlq())
                || name.equals(q.getNotificaciones()) || name.equals(q.getNotificacionesDlq())
                || name.equals(q.getReposicion()) || name.equals(q.getReposicionDlq())) {
            throw new BadRequestException("'" + name + "' es parte de la topología base del sistema y no puede eliminarse.");
        }
    }

    private Binding toBinding(BindingRequestDto requestDto) {
        Binding.DestinationType destType;
        try {
            destType = Binding.DestinationType.valueOf(requestDto.getDestinationType().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Tipo de destino inválido: " + requestDto.getDestinationType() + ". Valores permitidos: QUEUE, EXCHANGE.");
        }
        return new Binding(requestDto.getDestination(), destType, requestDto.getExchange(),
                requestDto.getRoutingKey(), requestDto.getArguments());
    }

    @Override
    public Map<String, Object> createQueue(QueueRequestDto requestDto) {
        validateName(requestDto.getName());
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
        validateName(queueName);
        assertNotSystemResource(queueName);
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
        validateName(requestDto.getName());
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
        validateName(exchangeName);
        assertNotSystemResource(exchangeName);
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

        Binding binding = toBinding(requestDto);

        amqpAdmin.declareBinding(binding);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Binding declarado exitosamente en RabbitMQ");
        response.put("destination", requestDto.getDestination());
        response.put("destinationType", binding.getDestinationType().name());
        response.put("exchange", requestDto.getExchange());
        response.put("routingKey", requestDto.getRoutingKey());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @Override
    public Map<String, Object> deleteBinding(BindingRequestDto requestDto) {
        log.info("[RABBIT ADMIN] Eliminando Binding entre destino '{}' ({}) y exchange '{}' con routing key '{}'",
                requestDto.getDestination(), requestDto.getDestinationType(), requestDto.getExchange(), requestDto.getRoutingKey());

        Binding binding = toBinding(requestDto);
        amqpAdmin.removeBinding(binding);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Binding eliminado exitosamente");
        response.put("destination", requestDto.getDestination());
        response.put("destinationType", binding.getDestinationType().name());
        response.put("exchange", requestDto.getExchange());
        response.put("routingKey", requestDto.getRoutingKey());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
    }
}

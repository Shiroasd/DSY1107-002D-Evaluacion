package com.retail.inventory.service;

import com.retail.inventory.dto.BindingRequestDto;
import com.retail.inventory.dto.ExchangeRequestDto;
import com.retail.inventory.dto.QueueRequestDto;

import java.util.Map;

/**
 * Servicio de administración dinámica de topologías de RabbitMQ.
 * Expone operaciones de creación y eliminación de colas, exchanges y enlaces usando AmqpAdmin.
 */
public interface RabbitAdminService {

    /**
     * Declara una nueva cola en RabbitMQ con soporte para DLX opcional.
     */
    Map<String, Object> createQueue(QueueRequestDto requestDto);

    /**
     * Elimina una cola existente en RabbitMQ.
     */
    Map<String, Object> deleteQueue(String queueName);

    /**
     * Declara un nuevo Exchange (DIRECT, TOPIC, FANOUT, HEADERS).
     */
    Map<String, Object> createExchange(ExchangeRequestDto requestDto);

    /**
     * Elimina un Exchange existente en RabbitMQ.
     */
    Map<String, Object> deleteExchange(String exchangeName);

    /**
     * Crea un enlace (Binding) entre una cola/exchange y un exchange de origen con una routing key.
     */
    Map<String, Object> createBinding(BindingRequestDto requestDto);
}

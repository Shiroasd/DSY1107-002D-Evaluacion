package com.retail.inventory.service;

import com.retail.inventory.dto.admin.CreateBindingRequestDto;
import com.retail.inventory.dto.admin.CreateExchangeRequestDto;
import com.retail.inventory.dto.admin.CreateQueueRequestDto;

import java.util.Map;

/**
 * Contrato de servicio para la administración dinámica de la topología RabbitMQ.
 * Expone operaciones de creación, eliminación y consulta de colas, exchanges y enlaces usando AmqpAdmin.
 */
public interface RabbitAdminService {

    /**
     * Declara una cola dinámica en el broker.
     */
    String createQueue(CreateQueueRequestDto dto);

    /**
     * Elimina una cola existente.
     */
    boolean deleteQueue(String queueName);

    /**
     * Purga los mensajes contenidos en una cola.
     */
    void purgeQueue(String queueName);

    /**
     * Declara un exchange dinámico (direct, topic, fanout, headers).
     */
    void createExchange(CreateExchangeRequestDto dto);

    /**
     * Elimina un exchange del broker.
     */
    boolean deleteExchange(String exchangeName);

    /**
     * Asocia una cola a un exchange mediante una clave de enrutamiento.
     */
    void createBinding(CreateBindingRequestDto dto);

    /**
     * Desvincula una cola de un exchange.
     */
    void deleteBinding(CreateBindingRequestDto dto);

    /**
     * Obtiene métricas y metadatos de una cola (ej. conteo de mensajes y consumidores).
     */
    Map<String, Object> getQueueInfo(String queueName);
}

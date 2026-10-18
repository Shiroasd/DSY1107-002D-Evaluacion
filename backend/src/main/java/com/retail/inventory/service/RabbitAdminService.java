package com.retail.inventory.service;

<<<<<<< HEAD
import com.retail.inventory.dto.admin.CreateBindingRequestDto;
import com.retail.inventory.dto.admin.CreateExchangeRequestDto;
import com.retail.inventory.dto.admin.CreateQueueRequestDto;
=======
import com.retail.inventory.dto.BindingRequestDto;
import com.retail.inventory.dto.ExchangeRequestDto;
import com.retail.inventory.dto.QueueRequestDto;
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48

import java.util.Map;

/**
<<<<<<< HEAD
 * Contrato de servicio para la administración dinámica de la topología RabbitMQ.
=======
 * Servicio de administración dinámica de topologías de RabbitMQ.
 * Expone operaciones de creación y eliminación de colas, exchanges y enlaces usando AmqpAdmin.
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
 */
public interface RabbitAdminService {

    /**
<<<<<<< HEAD
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
=======
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

    /**
     * Elimina un enlace (Binding) existente.
     */
    Map<String, Object> deleteBinding(BindingRequestDto requestDto);
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
}

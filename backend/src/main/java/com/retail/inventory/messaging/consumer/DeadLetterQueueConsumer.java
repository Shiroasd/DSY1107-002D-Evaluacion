package com.retail.inventory.messaging.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Consumidor especializado de la Dead Letter Queue (DLQ).
 * Captura mensajes fallidos desde la cola de incidentes y extrae encabezados de diagnóstico ('x-death')
 * para análisis forense, monitoreo y posterior resolución operacional.
 */
@Slf4j
@Component
public class DeadLetterQueueConsumer {

    @RabbitListener(
            queues = "${app.rabbitmq.queues.dead-letter:dead-letter}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            Object xDeathHeader = headers.get("x-death");

            log.error("==================== INCIDENTE CAPTURADO EN DLQ ====================");
            log.error("DeliveryTag: {}", deliveryTag);
            log.error("Payload del Mensaje: {}", payload);
            log.error("RoutingKey original: {}", message.getMessageProperties().getReceivedRoutingKey());
            log.error("Exchange origen: {}", message.getMessageProperties().getReceivedExchange());

            if (xDeathHeader instanceof List<?> xDeathList && !xDeathList.isEmpty()) {
                log.error("Diagnóstico 'x-death' detalles:");
                for (Object entry : xDeathList) {
                    if (entry instanceof Map<?, ?> deathMap) {
                        log.error(" -> Causa rechazo: {}, Cola: {}, Contador: {}, Hora: {}",
                                deathMap.get("reason"),
                                deathMap.get("queue"),
                                deathMap.get("count"),
                                deathMap.get("time"));
                    }
                }
            } else {
                log.warn("El mensaje en DLQ no posee encabezados 'x-death' adicionales.");
            }
            log.error("====================================================================");

            // Confirmamos el procesamiento del mensaje en la DLQ para mantener limpio el canal
            channel.basicAck(deliveryTag, false);
            log.info("Mensaje DLQ diagnosticado y confirmado (ACK) [deliveryTag={}]", deliveryTag);

        } catch (Exception ex) {
            log.error("Error crítico procesando mensaje desde DLQ [deliveryTag={}]: {}", deliveryTag, ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}

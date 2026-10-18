package com.retail.inventory.messaging.consumer;

import com.rabbitmq.client.Channel;
import com.retail.inventory.messaging.event.InventoryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de auditoría y trazabilidad histórica de eventos de inventario.
 * Registra logs estructurados para observabilidad y ejecuta confirmación manual de recepción.
 */
@Slf4j
@Component
public class AuditLogConsumer {

    @RabbitListener(
            queues = "${app.rabbitmq.queues.audit-log:audit-log}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleAuditLog(
            @Payload InventoryEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("[AUDIT-LOG] Evento recibido para auditoría: deliveryTag={}, eventId={}, type={}, productId={}, sku={}, timestamp={}",
                deliveryTag, event.getEventId(), event.getEventType(), event.getProductId(), event.getSku(), event.getTimestamp());

        try {
            // Confirmación explícita (ACK)
            channel.basicAck(deliveryTag, false);
            log.debug("[AUDIT-LOG] Mensaje de auditoría confirmado (ACK): deliveryTag={}", deliveryTag);
        } catch (Exception ex) {
            log.error("[AUDIT-LOG] Error procesando registro de auditoría. Derivando a DLQ: deliveryTag={}", deliveryTag, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}

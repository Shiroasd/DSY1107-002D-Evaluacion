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
 * Consumidor especializado en procesamiento de alertas de stock crítico.
 * Implementa confirmación manual explícita (basicAck) y rechazo sin reencolamiento (basicNack requeue=false)
 * ante incidentes críticos para derivación directa hacia la Dead Letter Queue (DLQ).
 */
@Slf4j
@Component
public class StockAlertConsumer {

    @RabbitListener(
            queues = "${app.rabbitmq.queues.stock-alert:stock-alert}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleStockAlert(
            @Payload InventoryEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Iniciando procesamiento de alerta de stock crítico: [deliveryTag={}, eventId={}, sku={}, stock={}]",
                deliveryTag, event.getEventId(), event.getSku(), event.getStock());

        try {
            if (event.getStock() == null || event.getStock() < 0) {
                throw new IllegalArgumentException("Stock inválido o negativo detectado en alerta: " + event.getStock());
            }

            log.warn("[ALERTA STOCK CRÍTICO] Producto ID={}, SKU='{}', Nombre='{}' ha alcanzado nivel crítico: {} unidades.",
                    event.getProductId(), event.getSku(), event.getName(), event.getStock());

            // Confirmación explícita positiva (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("Mensaje confirmado exitosamente (ACK): [deliveryTag={}, eventId={}]", deliveryTag, event.getEventId());

        } catch (Exception ex) {
            log.error("Error crítico procesando alerta de stock. Enviando a DLQ via basicNack(requeue=false): [deliveryTag={}, error={}]",
                    deliveryTag, ex.getMessage(), ex);

            // Rechazo explícito sin reencolamiento para derivar hacia la DLQ configurada en el exchange DLX
            channel.basicNack(deliveryTag, false, false);
        }
    }
}

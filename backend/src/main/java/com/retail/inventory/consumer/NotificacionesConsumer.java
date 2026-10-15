package com.retail.inventory.consumer;

import com.rabbitmq.client.Channel;
import com.retail.inventory.dto.StockModificadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de eventos especializado en Alertas y Notificaciones de Inventario.
 * Atiende la cola de notificaciones configurada dinámicamente en app.rabbitmq.queues.notificaciones.
 * Evalúa reglas de negocio (ej. stock crítico <= 3 unidades) e implementa confirmación manual (ACK/NACK con DLQ).
 */
@Component
@Slf4j
public class NotificacionesConsumer {

    private static final int UMBRAL_STOCK_CRITICO = 3;

    @RabbitListener(queues = "${app.rabbitmq.queues.notificaciones:inventario.notificaciones.queue}", ackMode = "MANUAL")
    public void procesarNotificacionStock(
            StockModificadoEvent evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("[ms-notificaciones] Mensaje recibido. DeliveryTag: {}", tag);

        try {
            if (evento == null) {
                throw new IllegalArgumentException("Payload de evento nulo en mensaje de notificaciones");
            }

            Integer stockActual = evento.getStockActual();

            // Evaluación de regla de negocio: Alerta si el stock es crítico (<= 3 unidades)
            if (stockActual != null && stockActual <= UMBRAL_STOCK_CRITICO) {
                log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                log.warn("[ms-notificaciones] ¡ALERTA DE STOCK CRÍTICO!");
                log.warn(" -> Producto: {} (SKU: {})", evento.getNombre(), evento.getSku());
                log.warn(" -> Stock Actual: {} unidades (Umbral Crítico: <= {})", stockActual, UMBRAL_STOCK_CRITICO);
                log.warn(" -> Responsable: {}", evento.getUsuarioResponsable());
                log.warn(" -> Disparando notificación urgente al Administrador y Jefe de Bodega...");
                log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            } else {
                log.info("[ms-notificaciones] Stock en nivel seguro ({} unidades). No se requiere alerta.", stockActual);
            }

            // Confirmación manual exitosa (ACK)
            channel.basicAck(tag, false);
            log.info("[ms-notificaciones] Mensaje confirmado exitosamente (ACK) para DeliveryTag: {}", tag);

        } catch (Exception ex) {
            log.error("[ms-notificaciones ERROR] Fallo al procesar notificación para DeliveryTag: {}. " +
                      "Enviando NACK con requeue=false hacia la Dead Letter Queue (DLQ). Causa: {}",
                      tag, ex.getMessage(), ex);

            // NACK con requeue = false para derivar a la Dead Letter Queue (DLQ)
            channel.basicNack(tag, false, false);
        }
    }
}

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
 * Consumidor de eventos especializado en Auditoría y Bitácora de Movimientos de Inventario.
 * Atiende la cola de auditoría configurada dinámicamente en app.rabbitmq.queues.auditoria.
 * Implementa confirmación manual (ACK/NACK) derivando mensajes con error hacia la DLQ (requeue = false).
 */
@Component
@Slf4j
public class AuditoriaConsumer {

    @RabbitListener(queues = "${app.rabbitmq.queues.auditoria:inventario.auditoria.queue}", ackMode = "MANUAL")
    public void registrarAuditoria(
            StockModificadoEvent evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("[ms-auditoria] Mensaje recibido. DeliveryTag: {}", tag);

        try {
            if (evento == null || evento.getSku() == null || evento.getSku().isBlank()) {
                throw new IllegalArgumentException("Payload de evento inválido o SKU nulo en mensaje de auditoría");
            }

            log.info("==================================================");
            log.info("[ms-auditoria] REGISTRANDO MOVIMIENTO EN BITÁCORA:");
            log.info(" -> Producto ID: {}", evento.getProductId());
            log.info(" -> SKU: {}", evento.getSku());
            log.info(" -> Producto: {}", evento.getNombre());
            log.info(" -> Stock Anterior: {}", evento.getStockAnterior());
            log.info(" -> Stock Actual: {}", evento.getStockActual());
            log.info(" -> Tipo de Movimiento: {}", evento.getTipoModificacion());
            log.info(" -> Fecha / Hora: {}", evento.getFechaMovimiento());
            log.info(" -> Usuario Responsable: {}", evento.getUsuarioResponsable());
            log.info("==================================================");

            // Confirmación manual exitosa (ACK)
            channel.basicAck(tag, false);
            log.info("[ms-auditoria] Mensaje confirmado exitosamente (ACK) para DeliveryTag: {}", tag);

        } catch (Exception ex) {
            log.error("[ms-auditoria ERROR] Fallo al procesar auditoría para DeliveryTag: {}. " +
                      "Enviando NACK con requeue=false hacia la Dead Letter Queue (DLQ). Causa: {}",
                      tag, ex.getMessage(), ex);

            // NACK con requeue = false para derivar a la Dead Letter Queue (DLQ)
            channel.basicNack(tag, false, false);
        }
    }
}

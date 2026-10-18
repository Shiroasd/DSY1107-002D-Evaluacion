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
 * Consumidor del dominio Auditoría: registra la bitácora de movimientos de inventario.
 * La cola se resuelve desde RabbitMQProperties (app.rabbitmq.queues.auditoria).
 */
@Component
@Slf4j
public class AuditoriaConsumer {

    private static final String NAME = "ms-auditoria";

    private final MessageAckHandler ackHandler;

    public AuditoriaConsumer(MessageAckHandler ackHandler) {
        this.ackHandler = ackHandler;
    }

    @RabbitListener(queues = "#{@rabbitMQProperties.queues.auditoria}", ackMode = "MANUAL")
    public void registrarAuditoria(
            StockModificadoEvent evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {

        log.info("[{}] Mensaje recibido. DeliveryTag: {}", NAME, tag);
        try {
            if (evento == null || evento.getSku() == null || evento.getSku().isBlank()) {
                throw new IllegalArgumentException("Payload de evento inválido o SKU nulo en mensaje de auditoría");
            }
            log.info("[{}] BITÁCORA -> productoId={}, sku={}, nombre={}, stock {} -> {}, tipo={}, fecha={}, usuario={}",
                    NAME, evento.getProductId(), evento.getSku(), evento.getNombre(), evento.getStockAnterior(),
                    evento.getStockActual(), evento.getTipoModificacion(), evento.getFechaMovimiento(),
                    evento.getUsuarioResponsable());
            ackHandler.ack(channel, tag, NAME);
        } catch (Exception ex) {
            ackHandler.fail(channel, tag, redelivered, ex, NAME);
        }
    }
}

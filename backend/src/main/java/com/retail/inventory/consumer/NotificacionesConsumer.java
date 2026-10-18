package com.retail.inventory.consumer;

import com.rabbitmq.client.Channel;
import com.retail.inventory.config.RabbitMQProperties;
import com.retail.inventory.dto.StockModificadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor del dominio Notificaciones: alertas por stock crítico.
 * El umbral y la cola provienen de RabbitMQProperties.
 */
@Component
@Slf4j
public class NotificacionesConsumer {

    private static final String NAME = "ms-notificaciones";

    private final MessageAckHandler ackHandler;
    private final RabbitMQProperties props;

    public NotificacionesConsumer(MessageAckHandler ackHandler, RabbitMQProperties props) {
        this.ackHandler = ackHandler;
        this.props = props;
    }

    @RabbitListener(queues = "#{@rabbitMQProperties.queues.notificaciones}", ackMode = "MANUAL")
    public void procesarNotificacionStock(
            StockModificadoEvent evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {

        log.info("[{}] Mensaje recibido. DeliveryTag: {}", NAME, tag);
        try {
            if (evento == null) {
                throw new IllegalArgumentException("Payload de evento nulo en mensaje de notificaciones");
            }
            Integer stockActual = evento.getStockActual();
            int umbral = props.getStockCriticoUmbral();

            if (stockActual != null && stockActual <= umbral) {
                log.warn("[{}] ¡ALERTA DE STOCK CRÍTICO! Producto: {} (SKU: {}), stock: {} (umbral <= {}), responsable: {}",
                        NAME, evento.getNombre(), evento.getSku(), stockActual, umbral, evento.getUsuarioResponsable());
            } else {
                log.info("[{}] Stock en nivel seguro ({}). No se requiere alerta.", NAME, stockActual);
            }
            ackHandler.ack(channel, tag, NAME);
        } catch (Exception ex) {
            ackHandler.fail(channel, tag, redelivered, ex, NAME);
        }
    }
}

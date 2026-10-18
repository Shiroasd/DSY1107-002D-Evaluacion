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
 * Consumidor del dominio Reposición: genera solicitudes de reposición cuando el stock queda crítico.
 * La cola se resuelve desde RabbitMQProperties (app.rabbitmq.queues.reposicion).
 */
@Component
@Slf4j
public class ReposicionConsumer {

    private static final String NAME = "ms-reposicion";

    private final MessageAckHandler ackHandler;

    public ReposicionConsumer(MessageAckHandler ackHandler) {
        this.ackHandler = ackHandler;
    }

    @RabbitListener(queues = "#{@rabbitMQProperties.queues.reposicion}", ackMode = "MANUAL")
    public void generarSolicitudReposicion(
            StockModificadoEvent evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {

        log.info("[{}] Mensaje recibido. DeliveryTag: {}", NAME, tag);
        try {
            if (evento == null || evento.getSku() == null || evento.getSku().isBlank() || evento.getStockActual() == null) {
                throw new IllegalArgumentException("Evento de reposición inválido: SKU o stock actual ausente");
            }
            log.info("[{}] SOLICITUD DE REPOSICIÓN generada -> sku={}, producto={}, stock actual={}",
                    NAME, evento.getSku(), evento.getNombre(), evento.getStockActual());
            ackHandler.ack(channel, tag, NAME);
        } catch (Exception ex) {
            ackHandler.fail(channel, tag, redelivered, ex, NAME);
        }
    }
}

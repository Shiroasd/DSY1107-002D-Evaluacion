package com.retail.inventory.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Política común de confirmación de mensajes para todos los consumidores.
 *
 *  - Éxito: ACK explícito.
 *  - Error NO recuperable (IllegalArgumentException: payload inválido): NACK sin requeue -> DLQ inmediata.
 *  - Error recuperable (cualquier otra excepción): un reintento (NACK con requeue); si el mensaje ya fue
 *    reentregado, NACK sin requeue -> DLQ.
 * Todo envío a DLQ queda registrado en logs.
 */
@Component
@Slf4j
public class MessageAckHandler {

    public void ack(Channel channel, long tag, String consumer) throws IOException {
        channel.basicAck(tag, false);
        log.info("[{}] Mensaje confirmado (ACK). DeliveryTag: {}", consumer, tag);
    }

    public void fail(Channel channel, long tag, boolean redelivered, Exception ex, String consumer) throws IOException {
        boolean recoverable = !(ex instanceof IllegalArgumentException);

        if (recoverable && !redelivered) {
            log.warn("[{}] Error recuperable. Reintentando (NACK requeue=true). DeliveryTag: {}. Causa: {}",
                    consumer, tag, ex.getMessage());
            channel.basicNack(tag, false, true);
            return;
        }

        log.error("[{}] Mensaje NO entregado -> DLQ (NACK requeue=false). DeliveryTag: {}. Tipo de error: {}. Causa: {}",
                consumer, tag, recoverable ? "recuperable (reintento agotado)" : "no recuperable", ex.getMessage(), ex);
        channel.basicNack(tag, false, false);
    }
}

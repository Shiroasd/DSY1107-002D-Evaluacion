package com.retail.inventory.consumer;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MessageAckHandlerTest {

    private final MessageAckHandler handler = new MessageAckHandler();
    private final Channel channel = mock(Channel.class);

    @Test
    @DisplayName("Procesamiento exitoso envía ACK explícito")
    void ackOnSuccess() throws IOException {
        handler.ack(channel, 1L, "test");
        verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Error no recuperable va directo a DLQ (NACK sin requeue)")
    void nonRecoverableGoesToDlq() throws IOException {
        handler.fail(channel, 2L, false, new IllegalArgumentException("payload inválido"), "test");
        verify(channel).basicNack(2L, false, false);
    }

    @Test
    @DisplayName("Error recuperable en primera entrega se reintenta (NACK con requeue)")
    void recoverableIsRetriedOnce() throws IOException {
        handler.fail(channel, 3L, false, new RuntimeException("BD no disponible"), "test");
        verify(channel).basicNack(3L, false, true);
    }

    @Test
    @DisplayName("Error recuperable ya reentregado va a DLQ (NACK sin requeue)")
    void recoverableRedeliveredGoesToDlq() throws IOException {
        handler.fail(channel, 4L, true, new RuntimeException("BD no disponible"), "test");
        verify(channel).basicNack(4L, false, false);
    }
}

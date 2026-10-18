package com.retail.inventory.messaging;

import com.rabbitmq.client.Channel;
import com.retail.inventory.messaging.consumer.AuditLogConsumer;
import com.retail.inventory.messaging.consumer.DeadLetterQueueConsumer;
import com.retail.inventory.messaging.consumer.StockAlertConsumer;
import com.retail.inventory.messaging.event.InventoryEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumerTest {

    @Mock
    private Channel channel;

    @InjectMocks
    private StockAlertConsumer stockAlertConsumer;

    @InjectMocks
    private AuditLogConsumer auditLogConsumer;

    @InjectMocks
    private DeadLetterQueueConsumer deadLetterQueueConsumer;

    @Test
    @DisplayName("StockAlertConsumer: Procesa alerta válida y emite basicAck")
    void stockAlertConsumerSuccessShouldAck() throws IOException {
        InventoryEvent event = InventoryEvent.builder()
                .eventId("evt-01")
                .eventType("STOCK_CRITICAL")
                .productId(1L)
                .sku("SKU-OK")
                .name("Producto Alerta")
                .stock(2)
                .timestamp(Instant.now())
                .build();

        long deliveryTag = 101L;

        stockAlertConsumer.handleStockAlert(event, channel, deliveryTag);

        verify(channel, times(1)).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("StockAlertConsumer: Ante error en payload emite basicNack sin requeue (DLQ)")
    void stockAlertConsumerErrorShouldNackWithoutRequeue() throws IOException {
        InventoryEvent invalidEvent = InventoryEvent.builder()
                .eventId("evt-err")
                .stock(-5) // Stock inválido negativo
                .build();

        long deliveryTag = 102L;

        stockAlertConsumer.handleStockAlert(invalidEvent, channel, deliveryTag);

        verify(channel, times(1)).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("AuditLogConsumer: Registra evento y confirma con basicAck")
    void auditLogConsumerSuccessShouldAck() throws IOException {
        InventoryEvent event = InventoryEvent.builder()
                .eventId("evt-audit")
                .eventType("PRODUCT_UPDATED")
                .productId(5L)
                .sku("SKU-AUDIT")
                .timestamp(Instant.now())
                .build();

        long deliveryTag = 201L;

        auditLogConsumer.handleAuditLog(event, channel, deliveryTag);

        verify(channel, times(1)).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("DeadLetterQueueConsumer: Extrae encabezados x-death y confirma con basicAck")
    void deadLetterQueueConsumerWithXDeathHeadersShouldAck() throws IOException {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey("product.stock.low");
        properties.setReceivedExchange("inventory.topic.exchange");

        Map<String, Object> deathHeaderEntry = Map.of(
                "reason", "rejected",
                "queue", "stock-alert",
                "count", 1L,
                "time", "2026-10-18T20:00:00Z"
        );
        properties.setHeader("x-death", List.of(deathHeaderEntry));

        Message message = new Message("{\"sku\":\"SKU-DLQ\"}".getBytes(StandardCharsets.UTF_8), properties);
        long deliveryTag = 301L;

        deadLetterQueueConsumer.handleDeadLetterMessage(message, channel, deliveryTag);

        verify(channel, times(1)).basicAck(deliveryTag, false);
    }
}

package com.retail.inventory.messaging;

import com.retail.inventory.config.RabbitMQProperties;
import com.retail.inventory.messaging.event.InventoryEvent;
import com.retail.inventory.messaging.publisher.InventoryEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMQProperties properties;
    private InventoryEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new RabbitMQProperties();
        publisher = new InventoryEventPublisher(rabbitTemplate, properties);
    }

    @Test
    @DisplayName("Debe publicar evento de stock crítico con routing key de stock bajo")
    void publishCriticalStockAlertShouldSendCorrectMessage() {
        publisher.publishCriticalStockAlert(10L, "SKU-CRIT-01", "Producto Crítico", 3);

        ArgumentCaptor<InventoryEvent> captor = ArgumentCaptor.forClass(InventoryEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(properties.getExchanges().getInventoryTopic()),
                eq(properties.getRoutingKeys().getProductStockLow()),
                captor.capture()
        );

        InventoryEvent event = captor.getValue();
        assertNotNull(event.getEventId());
        assertEquals("STOCK_CRITICAL", event.getEventType());
        assertEquals(10L, event.getProductId());
        assertEquals("SKU-CRIT-01", event.getSku());
        assertEquals(3, event.getStock());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Manejo defensivo: Indisponibilidad del broker no debe lanzar excepción")
    void publisherShouldHandleBrokerFailureDefensively() {
        doThrow(new AmqpConnectException(new java.net.ConnectException("Broker offline")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertDoesNotThrow(() ->
                publisher.publishCriticalStockAlert(20L, "SKU-FAIL", "Test Fallo", 1)
        );
    }
}

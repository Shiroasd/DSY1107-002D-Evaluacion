package com.retail.inventory;

import com.retail.inventory.config.RabbitMQProperties;
import com.retail.inventory.dto.InventarioAlertaEvent;
import com.retail.inventory.dto.InventarioMovimientoEvent;
import com.retail.inventory.service.impl.InventarioAlertaProducer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.retail.inventory.config.InventarioKafkaConfig.TOPIC_ALERTAS;
import static org.mockito.Mockito.*;

class InventarioAlertaProducerTest {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, InventarioAlertaEvent> kafka = mock(KafkaTemplate.class);
    private final RabbitMQProperties properties = new RabbitMQProperties();
    private final InventarioAlertaProducer producer = new InventarioAlertaProducer(kafka, properties);

    @Test
    void fiveUnitsDoNotTriggerTheDefaultThreshold() {
        producer.publicar(movement(5));
        verifyNoInteractions(kafka);
    }

    @Test
    void publishesExactlyOnceAtDefaultBoundary() {
        assertPublished(movement(3), 3);
    }

    @Test
    void respectsAnExplicitThreshold() {
        properties.setStockCriticoUmbral(5);
        assertPublished(movement(5), 5);
    }

    private void assertPublished(InventarioMovimientoEvent event, int threshold) {
        var alert = new InventarioAlertaEvent(event.eventId(), event.productoId(), event.sku(), event.nombre(),
            event.stockNuevo(), threshold, event.fecha(), event.usuario());
        when(kafka.send(TOPIC_ALERTAS, "1", alert)).thenReturn(new CompletableFuture<>());
        producer.publicar(event);
        verify(kafka).send(TOPIC_ALERTAS, "1", alert);
        verifyNoMoreInteractions(kafka);
    }

    private InventarioMovimientoEvent movement(int stock) {
        return new InventarioMovimientoEvent(UUID.randomUUID(), 1L, "SKU-01", "Producto",
            17, stock, stock - 17, InventarioMovimientoEvent.Tipo.SALIDA, Instant.now(), "usuario");
    }
}

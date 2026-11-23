package com.retail.inventory.service.impl;

import com.retail.inventory.dto.InventarioMovimientoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.retail.inventory.config.InventarioKafkaConfig.TOPIC;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class InventarioMovimientoProducer {
    private final KafkaTemplate<String, InventarioMovimientoEvent> kafkaTemplate;

    @TransactionalEventListener
    public void publicar(InventarioMovimientoEvent event) {
        // shortcut: un fallo tras el commit puede perder el evento, usar outbox si se exige entrega durable.
        try {
            kafkaTemplate.send(TOPIC, event.productoId().toString(), event).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Fallo Kafka eventId={} productoId={}", event.eventId(), event.productoId(), error);
                } else {
                    log.info("Movimiento publicado eventId={} productoId={} partition={} offset={}",
                        event.eventId(), event.productoId(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
        } catch (RuntimeException error) {
            log.error("Fallo al iniciar envio Kafka eventId={} productoId={}", event.eventId(), event.productoId(), error);
        }
    }
}

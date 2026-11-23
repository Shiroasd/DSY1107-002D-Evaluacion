package com.retail.inventory.service.impl;

import com.retail.inventory.config.RabbitMQProperties;
import com.retail.inventory.dto.InventarioAlertaEvent;
import com.retail.inventory.dto.InventarioMovimientoEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.retail.inventory.config.InventarioKafkaConfig.TOPIC_ALERTAS;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class InventarioAlertaProducer {
    private final KafkaTemplate<String, InventarioAlertaEvent> kafkaTemplate;
    private final RabbitMQProperties properties;

    @PostConstruct
    void registrarConfiguracion() {
        log.info("Alertas Kafka habilitadas topic={} umbralCritico={}", TOPIC_ALERTAS, properties.getStockCriticoUmbral());
    }

    @TransactionalEventListener
    public void publicar(InventarioMovimientoEvent movement) {
        int umbral = properties.getStockCriticoUmbral();
        if (movement.stockNuevo() > umbral) {
            log.debug("Alerta Kafka omitida eventId={} productoId={} stock={} umbral={}",
                movement.eventId(), movement.productoId(), movement.stockNuevo(), umbral);
            return;
        }

        var event = new InventarioAlertaEvent(movement.eventId(), movement.productoId(), movement.sku(),
            movement.nombre(), movement.stockNuevo(), umbral, movement.fecha(), movement.usuario());
        try {
            kafkaTemplate.send(TOPIC_ALERTAS, event.productoId().toString(), event).whenComplete((result, error) -> {
                if (error != null) {
                    registrarFallo(event, error);
                } else {
                    var metadata = result.getRecordMetadata();
                    log.info("Alerta Kafka publicada eventId={} productoId={} partition={} offset={}",
                        event.eventId(), event.productoId(), metadata.partition(), metadata.offset());
                }
            });
        } catch (RuntimeException error) {
            registrarFallo(event, error);
        }
    }

    private void registrarFallo(InventarioAlertaEvent event, Throwable error) {
        log.error("Fallo al publicar alerta Kafka eventId={} productoId={} error={}",
            event.eventId(), event.productoId(), error.getClass().getSimpleName());
    }
}

package com.retail.inventory.consumer;

import com.retail.inventory.dto.InventarioMovimientoEvent;
import com.retail.inventory.service.MovimientoInventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.retail.inventory.config.InventarioKafkaConfig.TOPIC;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class InventarioMovimientoConsumer {
    private final InventarioMovimientoValidator validator;
    private final MovimientoInventarioService service;

    @KafkaListener(topics = TOPIC, groupId = "${app.kafka.group-id}")
    public void consumir(ConsumerRecord<String, InventarioMovimientoEvent> record) {
        InventarioMovimientoEvent event = validator.validar(record);
        boolean registrado = service.registrar(event);
        log.info("Movimiento Kafka eventId={} productoId={} registrado={}", event.eventId(), event.productoId(), registrado);
    }
}

package com.retail.inventory.consumer;

import com.retail.inventory.dto.InventarioAlertaEvent;
import com.retail.inventory.service.AlertaInventarioService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.retail.inventory.config.InventarioKafkaConfig.TOPIC_ALERTAS;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class InventarioAlertaConsumer {
    private final Validator validator;
    private final AlertaInventarioService service;

    @KafkaListener(topics = TOPIC_ALERTAS, groupId = "${app.kafka.alertas-group-id}",
        properties = "spring.json.value.default.type=com.retail.inventory.dto.InventarioAlertaEvent")
    public void consumir(ConsumerRecord<String, InventarioAlertaEvent> record) {
        InventarioAlertaEvent event = record.value();
        if (event == null || !validator.validate(event).isEmpty()
            || !event.productoId().toString().equals(record.key())
            || event.stockActual() > event.umbral()) {
            throw new IllegalArgumentException("Alerta de inventario invalida");
        }

        boolean registrado = service.registrar(event);
        log.info("Alerta Kafka procesada eventId={} productoId={} stock={} umbral={} registrada={}",
            event.eventId(), event.productoId(), event.stockActual(), event.umbral(), registrado);
    }
}

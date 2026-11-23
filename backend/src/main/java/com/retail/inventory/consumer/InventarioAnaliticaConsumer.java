package com.retail.inventory.consumer;

import com.retail.inventory.dto.InventarioMovimientoEvent;
import com.retail.inventory.service.AnaliticaInventarioService;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import static com.retail.inventory.config.InventarioKafkaConfig.TOPIC;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class InventarioAnaliticaConsumer {
    public static final String LISTENER_ID = "inventario-analitica";

    private final InventarioMovimientoValidator validator;
    private final AnaliticaInventarioService service;
    private final RetryTemplate retry = RetryTemplate.builder()
        .maxAttempts(3)
        .fixedBackoff(1000)
        .retryOn(DataAccessException.class)
        .retryOn(PersistenceException.class)
        .retryOn(TransactionException.class)
        .build();

    @KafkaListener(id = LISTENER_ID, topics = TOPIC, groupId = "${app.kafka.analitica-group-id}",
        containerFactory = "analiticaKafkaListenerContainerFactory")
    public void consumir(ConsumerRecord<String, InventarioMovimientoEvent> record) {
        var event = validator.validar(record);
        boolean aplicado;
        try {
            aplicado = retry.execute(context -> service.procesar(event));
        } catch (RuntimeException error) {
            log.error("Analitica detenida sin confirmar topic={} partition={} offset={} error={}",
                record.topic(), record.partition(), record.offset(), error.getClass().getSimpleName());
            throw error;
        }
        log.info("Analitica Kafka eventId={} productoId={} aplicado={}", event.eventId(), event.productoId(), aplicado);
    }
}

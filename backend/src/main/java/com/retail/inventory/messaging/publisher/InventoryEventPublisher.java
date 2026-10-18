package com.retail.inventory.messaging.publisher;

import com.retail.inventory.config.RabbitMQProperties;
import com.retail.inventory.messaging.event.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicador de eventos de inventario hacia el broker RabbitMQ.
 * Implementa manejo defensivo de excepciones para asegurar que una indisponibilidad
 * del broker AMQP nunca aborte o degrade las transacciones en base de datos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties rabbitMQProperties;

    /**
     * Emite una alerta de stock crítico hacia el exchange temático con routing key de stock bajo.
     *
     * @param productId identificador único del producto
     * @param sku código SKU
     * @param name nombre del producto
     * @param stock cantidad actual en stock (crítico <= 5)
     */
    public void publishCriticalStockAlert(Long productId, String sku, String name, Integer stock) {
        InventoryEvent event = InventoryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("STOCK_CRITICAL")
                .productId(productId)
                .sku(sku)
                .name(name)
                .stock(stock)
                .timestamp(Instant.now())
                .build();

        String exchange = rabbitMQProperties.getExchanges().getInventoryTopic();
        String routingKey = rabbitMQProperties.getRoutingKeys().getProductStockLow();

        publish(exchange, routingKey, event);
    }

    /**
     * Emisión genérica con captura defensiva de fallos de conectividad o broker.
     */
    public void publish(String exchange, String routingKey, InventoryEvent event) {
        try {
            log.debug("Publicando evento AMQP hacia exchange '{}' con routingKey '{}': {}", exchange, routingKey, event);
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Evento AMQP emitido con éxito [eventId={}, type={}, routingKey={}]",
                    event.getEventId(), event.getEventType(), routingKey);
        } catch (Exception ex) {
            log.error("Fallo no bloqueante al publicar evento en RabbitMQ [eventId={}, routingKey={}]: {}",
                    event.getEventId(), routingKey, ex.getMessage(), ex);
        }
    }
}

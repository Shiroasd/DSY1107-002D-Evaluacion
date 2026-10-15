package com.retail.inventory.service.impl;

import com.retail.inventory.dto.StockModificadoEvent;
import com.retail.inventory.service.InventoryEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementación de mensajería asíncrona con Spring AMQP y RabbitMQ.
 * Centraliza la publicación de eventos leyendo dinámicamente las propiedades de application.yml.
 * Respeta el principio de aislamiento: la capa de servicio solo emite eventos sin acoplarse
 * a la topología interna del broker.
 */
@Service
@Slf4j
public class RabbitInventoryEventPublisherImpl implements InventoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:inventario.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-keys.stock-modificado:inventario.stock.modificado}")
    private String routingKey;

    @Autowired
    public RabbitInventoryEventPublisherImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishStockModificado(StockModificadoEvent event) {
        try {
            log.info("[ASÍNCRONO] Publicando evento de stock modificado hacia Exchange: '{}', RoutingKey: '{}', SKU: '{}'",
                    exchange, routingKey, event.getSku());

            rabbitTemplate.convertAndSend(exchange, routingKey, event);

            log.info("[ASÍNCRONO] Evento publicado exitosamente en RabbitMQ para SKU: '{}'", event.getSku());
        } catch (AmqpException ex) {
            log.error("[ASÍNCRONO ERROR] No fue posible publicar el evento en RabbitMQ para SKU: '{}'. Causa: {}",
                    event.getSku(), ex.getMessage(), ex);
        }
    }
}

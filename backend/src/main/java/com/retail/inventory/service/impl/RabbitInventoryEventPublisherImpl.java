package com.retail.inventory.service.impl;

import com.retail.inventory.config.RabbitMQProperties;
import com.retail.inventory.dto.StockModificadoEvent;
import com.retail.inventory.service.InventoryEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publicación asíncrona de eventos con Spring AMQP. Nombres de exchange y routing keys vienen de
 * RabbitMQProperties; un fallo del broker se registra y no afecta la operación síncrona de negocio.
 */
@Service
@Slf4j
public class RabbitInventoryEventPublisherImpl implements InventoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties props;

    public RabbitInventoryEventPublisherImpl(RabbitTemplate rabbitTemplate, RabbitMQProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @Override
    public void publishStockModificado(StockModificadoEvent event) {
        send(props.getRoutingKeys().getStockModificado(), event);

        Integer stock = event.getStockActual();
        if (stock != null && stock <= props.getStockCriticoUmbral()) {
            send(props.getRoutingKeys().getStockCritico(), event);
        }
    }

    private void send(String routingKey, StockModificadoEvent event) {
        try {
            log.info("[ASÍNCRONO] Publicando evento -> exchange '{}', routingKey '{}', SKU '{}'",
                    props.getExchange(), routingKey, event.getSku());
            rabbitTemplate.convertAndSend(props.getExchange(), routingKey, event);
        } catch (AmqpException ex) {
            log.error("[ASÍNCRONO ERROR] No fue posible publicar en RabbitMQ (routingKey '{}', SKU '{}'). Causa: {}",
                    routingKey, event.getSku(), ex.getMessage(), ex);
        }
    }
}

package com.retail.inventory.service;

import com.retail.inventory.dto.StockModificadoEvent;

/**
 * Puerto de salida para publicación de eventos de dominio de inventario.
 * Desacopla la lógica de negocio de la tecnología concreta de mensajería (RabbitMQ).
 */
public interface InventoryEventPublisher {

    /**
     * Publica el evento de stock modificado (auditoría y notificaciones). Si el stock resultante
     * es crítico, además emite el evento de stock crítico (reposición).
     */
    void publishStockModificado(StockModificadoEvent event);
}

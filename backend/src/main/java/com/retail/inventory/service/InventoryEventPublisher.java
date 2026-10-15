package com.retail.inventory.service;

import com.retail.inventory.dto.StockModificadoEvent;

/**
 * Puerto de salida para publicación de eventos de dominio de inventario.
 * Desacopla la lógica de negocio de la tecnología concreta de mensajería (RabbitMQ).
 */
public interface InventoryEventPublisher {

    /**
     * Publica el evento de stock modificado para ser procesado asíncronamente por auditoría y notificaciones.
     * @param event Datos del evento de modificación de stock.
     */
    void publishStockModificado(StockModificadoEvent event);
}

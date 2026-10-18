package com.retail.inventory.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Evento de dominio para trazabilidad y alertas de inventario.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador único de trazabilidad del evento.
     */
    private String eventId;

    /**
     * Tipo de evento (ej. 'STOCK_CRITICAL', 'PRODUCT_CREATED', 'PRODUCT_UPDATED').
     */
    private String eventType;

    /**
     * Identificador del producto afectado.
     */
    private Long productId;

    /**
     * Código SKU del producto.
     */
    private String sku;

    /**
     * Nombre comercial del producto.
     */
    private String name;

    /**
     * Nivel de stock actual registrado.
     */
    private Integer stock;

    /**
     * Marca de tiempo UTC del evento.
     */
    private Instant timestamp;
}

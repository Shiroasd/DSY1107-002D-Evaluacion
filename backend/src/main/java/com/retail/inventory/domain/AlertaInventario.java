package com.retail.inventory.domain;

import com.retail.inventory.dto.InventarioAlertaEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alertas_inventario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertaInventario {
    @Id
    private UUID eventId;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private int stockActual;

    @Column(nullable = false)
    private int umbral;

    @Column(nullable = false)
    private Instant fecha;

    private String usuarioResponsable;

    public AlertaInventario(InventarioAlertaEvent event) {
        eventId = event.eventId();
        productoId = event.productoId();
        sku = event.sku();
        nombre = event.nombre();
        stockActual = event.stockActual();
        umbral = event.umbral();
        fecha = event.fecha();
        usuarioResponsable = event.usuarioResponsable();
    }
}

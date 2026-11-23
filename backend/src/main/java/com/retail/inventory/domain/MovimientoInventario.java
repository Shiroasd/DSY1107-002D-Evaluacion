package com.retail.inventory.domain;

import com.retail.inventory.dto.InventarioMovimientoEvent;
import com.retail.inventory.dto.InventarioMovimientoEvent.Tipo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimientos_inventario", indexes =
    @Index(name = "idx_movimiento_producto_fecha", columnList = "productoId, fecha"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoInventario {
    @Id
    private UUID eventId;

    @Column(nullable = false)
    private Long productoId;

    @Column(length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private int stockAnterior;

    @Column(nullable = false)
    private int stockNuevo;

    @Column(nullable = false)
    private int diferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(nullable = false)
    private Instant fecha;

    private String usuario;

    public MovimientoInventario(InventarioMovimientoEvent event) {
        eventId = event.eventId();
        productoId = event.productoId();
        sku = event.sku();
        nombre = event.nombre();
        stockAnterior = event.stockAnterior();
        stockNuevo = event.stockNuevo();
        diferencia = event.diferencia();
        tipo = event.tipo();
        fecha = event.fecha();
        usuario = event.usuario();
    }
}

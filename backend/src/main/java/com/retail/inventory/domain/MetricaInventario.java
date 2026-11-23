package com.retail.inventory.domain;

import com.retail.inventory.dto.InventarioMovimientoEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "metricas_inventario", indexes = @Index(name = "idx_metricas_movimientos", columnList = "movimientos, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetricaInventario {
    public static final long RESUMEN_ID = 0L;

    @Id
    private Long id;
    private String sku;
    private String nombre;
    private long movimientos;
    private long entradas;
    private long salidas;
    private long unidadesIngresadas;
    private long unidadesRetiradas;
    private Instant ultimoMovimiento;
    private Instant ultimoProcesamiento;

    public MetricaInventario(long id) {
        this.id = id;
    }

    public void acumular(InventarioMovimientoEvent event, Instant procesado) {
        movimientos++;
        long variacion = (long) event.stockNuevo() - event.stockAnterior();
        if (variacion > 0) {
            entradas++;
            unidadesIngresadas += variacion;
        } else if (variacion < 0) {
            salidas++;
            unidadesRetiradas -= variacion;
        }

        if (ultimoMovimiento == null || !event.fecha().isBefore(ultimoMovimiento)) {
            ultimoMovimiento = event.fecha();
            if (id != RESUMEN_ID) {
                sku = event.sku();
                nombre = event.nombre();
            }
        }
        ultimoProcesamiento = procesado;
    }
}

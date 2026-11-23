package com.retail.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record InventarioMovimientoEvent(
    @NotNull UUID eventId,
    @NotNull @Positive Long productoId,
    @Size(max = 50) String sku,
    @NotBlank @Size(max = 150) String nombre,
    @NotNull @PositiveOrZero Integer stockAnterior,
    @NotNull @PositiveOrZero Integer stockNuevo,
    @NotNull Integer diferencia,
    @NotNull Tipo tipo,
    @NotNull Instant fecha,
    @Size(max = 255) String usuario
) {
    public enum Tipo { ENTRADA, SALIDA, AJUSTE, CREACION }
}

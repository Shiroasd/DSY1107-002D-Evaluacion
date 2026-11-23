
package com.retail.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record InventarioAlertaEvent(
    @NotNull UUID eventId,
    @NotNull @Positive Long productoId,
    @NotBlank @Size(max = 50) String sku,
    @NotBlank @Size(max = 150) String nombre,
    @NotNull @PositiveOrZero Integer stockActual,
    @NotNull @PositiveOrZero Integer umbral,
    @NotNull Instant fecha,
    @Size(max = 255) String usuarioResponsable
) {
}

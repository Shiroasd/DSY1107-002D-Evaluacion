package com.retail.inventory.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la creación dinámica y validada de exchanges en RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExchangeRequestDto {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = "^(?i)(direct|topic|fanout|headers)$", message = "El tipo de exchange debe ser 'direct', 'topic', 'fanout' o 'headers'")
    private String type;

    @Builder.Default
    private boolean durable = true;

    @Builder.Default
    private boolean autoDelete = false;
}

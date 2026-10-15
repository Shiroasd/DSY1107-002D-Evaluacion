package com.retail.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO para la creación dinámica de enlaces (bindings) en RabbitMQ mediante el módulo de administración.
 * Incluye validaciones Jakarta (@NotBlank, @Pattern) según la pauta de evaluación.
 */
@Data
@Builder
public class BindingRequestDto {

    @NotBlank(message = "El nombre de la entidad destino es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El destino solo puede contener caracteres alfanuméricos, puntos, guiones y guiones bajos")
    private String destination;

    @NotBlank(message = "El tipo de destino es obligatorio (QUEUE o EXCHANGE)")
    @Pattern(regexp = "^(?i)(QUEUE|EXCHANGE)$", message = "El tipo de destino debe ser QUEUE o EXCHANGE")
    private String destinationType;

    @NotBlank(message = "El nombre del exchange de origen es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El exchange solo puede contener caracteres alfanuméricos, puntos, guiones y guiones bajos")
    private String exchange;

    @NotBlank(message = "La clave de enrutamiento (routingKey) es obligatoria")
    private String routingKey;

    private Map<String, Object> arguments;

    public BindingRequestDto() {
    }

    public BindingRequestDto(String destination, String destinationType, String exchange, String routingKey, Map<String, Object> arguments) {
        this.destination = destination;
        this.destinationType = destinationType;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.arguments = arguments;
    }
}

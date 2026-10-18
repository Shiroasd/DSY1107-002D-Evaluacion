package com.retail.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO para la creación dinámica de exchanges en RabbitMQ mediante el módulo de administración.
 */
@Data
@Builder
public class ExchangeRequestDto {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Size(max = 255, message = "El nombre del exchange no puede superar 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange solo puede contener caracteres alfanuméricos, puntos, guiones y guiones bajos")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio (DIRECT, TOPIC, FANOUT, HEADERS)")
    @Pattern(regexp = "^(?i)(DIRECT|TOPIC|FANOUT|HEADERS)$", message = "El tipo de exchange debe ser DIRECT, TOPIC, FANOUT o HEADERS")
    private String type;

    @Builder.Default
    private boolean durable = true;

    @Builder.Default
    private boolean autoDelete = false;

    private Map<String, Object> arguments;

    public ExchangeRequestDto() {
    }

    public ExchangeRequestDto(String name, String type, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        this.name = name;
        this.type = type;
        this.durable = durable;
        this.autoDelete = autoDelete;
        this.arguments = arguments;
    }
}

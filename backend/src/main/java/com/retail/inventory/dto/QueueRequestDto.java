package com.retail.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO para la creación dinámica de colas en RabbitMQ mediante el módulo de administración.
 */
@Data
@Builder
public class QueueRequestDto {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola solo puede contener caracteres alfanuméricos, puntos, guiones y guiones bajos")
    private String name;

    @Builder.Default
    private boolean durable = true;

    @Builder.Default
    private boolean exclusive = false;

    @Builder.Default
    private boolean autoDelete = false;

    private String deadLetterExchange;

    private String deadLetterRoutingKey;

    private Map<String, Object> arguments;

    public QueueRequestDto() {
    }

    public QueueRequestDto(String name, boolean durable, boolean exclusive, boolean autoDelete, String deadLetterExchange, String deadLetterRoutingKey, Map<String, Object> arguments) {
        this.name = name;
        this.durable = durable;
        this.exclusive = exclusive;
        this.autoDelete = autoDelete;
        this.deadLetterExchange = deadLetterExchange;
        this.deadLetterRoutingKey = deadLetterRoutingKey;
        this.arguments = arguments;
    }
}

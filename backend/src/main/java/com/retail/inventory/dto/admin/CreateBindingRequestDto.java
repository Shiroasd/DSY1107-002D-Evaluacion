package com.retail.inventory.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la asociación dinámica (Binding) entre colas y exchanges en RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBindingRequestDto {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola contiene caracteres no válidos")
    private String queueName;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange contiene caracteres no válidos")
    private String exchangeName;

    @NotBlank(message = "La clave de enrutamiento (routing key) es obligatoria")
    private String routingKey;
}

package com.retail.inventory.controller;

import com.retail.inventory.dto.admin.CreateBindingRequestDto;
import com.retail.inventory.dto.admin.CreateExchangeRequestDto;
import com.retail.inventory.dto.admin.CreateQueueRequestDto;
import com.retail.inventory.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para administración dinámica de la topología RabbitMQ.
 * Protegido exclusivamente para usuarios autenticados con rol 'Admin' extraído del token JWT.
 */
@RestController
@RequestMapping("/api/v1/admin/rabbitmq")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin', 'ADMIN') or hasAuthority('SCOPE_OT.Admin')")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    /**
     * Declara una nueva cola en RabbitMQ.
     */
    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> createQueue(@Valid @RequestBody CreateQueueRequestDto requestDto) {
        String queueName = rabbitAdminService.createQueue(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Cola declarada exitosamente", "queue", queueName));
    }

    /**
     * Consulta información y métricas de una cola.
     */
    @GetMapping("/queues/{queueName}")
    public ResponseEntity<Map<String, Object>> getQueueInfo(@PathVariable String queueName) {
        return ResponseEntity.ok(rabbitAdminService.getQueueInfo(queueName));
    }

    /**
     * Elimina una cola existente en RabbitMQ.
     */
    @DeleteMapping("/queues/{queueName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteQueue(@PathVariable String queueName) {
        rabbitAdminService.deleteQueue(queueName);
        return ResponseEntity.noContent().build();
    }

    /**
     * Purga todos los mensajes contenidos en una cola.
     */
    @PostMapping("/queues/{queueName}/purge")
    public ResponseEntity<Map<String, String>> purgeQueue(@PathVariable String queueName) {
        rabbitAdminService.purgeQueue(queueName);
        return ResponseEntity.ok(Map.of("message", "Cola purgada exitosamente", "queue", queueName));
    }

    /**
     * Declara un nuevo exchange dinámico en RabbitMQ.
     */
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> createExchange(@Valid @RequestBody CreateExchangeRequestDto requestDto) {
        rabbitAdminService.createExchange(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Exchange declarado exitosamente", "exchange", requestDto.getName()));
    }

    /**
     * Elimina un exchange existente.
     */
    @DeleteMapping("/exchanges/{exchangeName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteExchange(@PathVariable String exchangeName) {
        rabbitAdminService.deleteExchange(exchangeName);
        return ResponseEntity.noContent().build();
    }

    /**
     * Asocia una cola a un exchange con una clave de enrutamiento.
     */
    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> createBinding(@Valid @RequestBody CreateBindingRequestDto requestDto) {
        rabbitAdminService.createBinding(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Binding creado exitosamente",
                        "queue", requestDto.getQueueName(),
                        "exchange", requestDto.getExchangeName(),
                        "routingKey", requestDto.getRoutingKey()));
    }

    /**
     * Elimina la asociación entre una cola y un exchange.
     */
    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteBinding(@Valid @RequestBody CreateBindingRequestDto requestDto) {
        rabbitAdminService.deleteBinding(requestDto);
        return ResponseEntity.noContent().build();
    }
}

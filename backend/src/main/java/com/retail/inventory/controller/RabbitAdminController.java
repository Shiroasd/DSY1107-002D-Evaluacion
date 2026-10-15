package com.retail.inventory.controller;

import com.retail.inventory.dto.BindingRequestDto;
import com.retail.inventory.dto.ExchangeRequestDto;
import com.retail.inventory.dto.QueueRequestDto;
import com.retail.inventory.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la administración dinámica de componentes de RabbitMQ (Microservicio Administrador).
 * Protegido estrictamente para usuarios con rol 'Admin' mediante Microsoft Entra ID.
 */
@RestController
@RequestMapping("/api/v1/admin/rabbitmq")
@PreAuthorize("hasAnyRole('Admin', 'ADMIN')")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    @Autowired
    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    /**
     * Declara dinámicamente una nueva cola en RabbitMQ.
     */
    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> createQueue(@Valid @RequestBody QueueRequestDto requestDto) {
        Map<String, Object> result = rabbitAdminService.createQueue(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Elimina una cola existente en RabbitMQ.
     */
    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Map<String, Object>> deleteQueue(@PathVariable String name) {
        Map<String, Object> result = rabbitAdminService.deleteQueue(name);
        return ResponseEntity.ok(result);
    }

    /**
     * Declara dinámicamente un nuevo Exchange en RabbitMQ.
     */
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> createExchange(@Valid @RequestBody ExchangeRequestDto requestDto) {
        Map<String, Object> result = rabbitAdminService.createExchange(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Elimina un Exchange existente en RabbitMQ.
     */
    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Map<String, Object>> deleteExchange(@PathVariable String name) {
        Map<String, Object> result = rabbitAdminService.deleteExchange(name);
        return ResponseEntity.ok(result);
    }

    /**
     * Declara dinámicamente un Binding (enlace) entre colas/exchanges en RabbitMQ.
     */
    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> createBinding(@Valid @RequestBody BindingRequestDto requestDto) {
        Map<String, Object> result = rabbitAdminService.createBinding(requestDto);
        return ResponseEntity.ok(result);
    }
}

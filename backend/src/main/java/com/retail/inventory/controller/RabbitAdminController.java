package com.retail.inventory.controller;

<<<<<<< HEAD
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
=======
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
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48

import java.util.Map;

/**
<<<<<<< HEAD
 * Controlador REST para administración dinámica de la topología RabbitMQ.
 * Protegido exclusivamente para usuarios autenticados con rol 'Admin' extraído del token JWT.
 */
@RestController
@RequestMapping("/api/v1/admin/rabbitmq")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Admin', 'ADMIN') or hasAuthority('SCOPE_OT.Admin')")
=======
 * Controlador REST para la administración dinámica de componentes de RabbitMQ (Microservicio Administrador).
 * Protegido estrictamente para usuarios con rol 'Admin' mediante Microsoft Entra ID.
 */
@RestController
@RequestMapping("/api/v1/admin/rabbitmq")
@PreAuthorize("hasAnyRole('Admin', 'ADMIN')")
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

<<<<<<< HEAD
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
=======
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
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
    }

    /**
     * Elimina una cola existente en RabbitMQ.
     */
<<<<<<< HEAD
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
=======
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
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Elimina un Binding. Los datos se envían como parámetros de query:
     * DELETE /bindings?destination=...&destinationType=QUEUE&exchange=...&routingKey=...
     */
    @DeleteMapping("/bindings")
    public ResponseEntity<Map<String, Object>> deleteBinding(@Valid BindingRequestDto requestDto) {
        Map<String, Object> result = rabbitAdminService.deleteBinding(requestDto);
        return ResponseEntity.ok(result);
>>>>>>> a1092769bc47f2cd7aa206ecf02b383a12d4ab48
    }
}

package com.retail.inventory.controller;

import com.retail.inventory.exception.BadRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/v1/admin/kafka/topics")
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KafkaAdminController {
    private static final String MANAGED_NAME = "demo[.][a-zA-Z0-9][a-zA-Z0-9._-]{0,243}";
    private final Admin admin;

    @Value("${app.kafka.allow-topic-deletion:false}")
    private boolean allowTopicDeletion;

    @GetMapping
    public List<String> listar() throws ExecutionException, InterruptedException, TimeoutException {
        return esperar(admin.listTopics().names()).stream().sorted().toList();
    }

    @GetMapping("/{name}")
    public TopicDetails consultar(@PathVariable String name)
        throws ExecutionException, InterruptedException, TimeoutException {
        if (!name.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,248}")) {
            throw new BadRequestException("Nombre de topico invalido");
        }
        var topic = esperar(admin.describeTopics(List.of(name)).topicNameValues().get(name));
        var resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        var config = esperar(admin.describeConfigs(List.of(resource)).values().get(resource));
        var partitions = topic.partitions().stream().map(partition -> new PartitionDetails(
            partition.partition(), partition.leader() == null || partition.leader().isEmpty()
                ? null : partition.leader().id(),
            partition.replicas().stream().map(Node::id).toList(),
            partition.isr().stream().map(Node::id).toList()
        )).toList();
        return new TopicDetails(name, partitions, config.get(TopicConfig.RETENTION_MS_CONFIG).value(),
            config.get(TopicConfig.CLEANUP_POLICY_CONFIG).value());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> crear(@Valid @RequestBody CreateTopic request)
        throws ExecutionException, InterruptedException, TimeoutException {
        var topic = new NewTopic(request.name(), request.partitions(), request.replicationFactor())
            .configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, request.retentionMs().toString(),
                TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE));
        esperar(admin.createTopics(List.of(topic)).all());
        return Map.of("name", request.name());
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String name, @RequestParam(required = false) String confirm)
        throws ExecutionException, InterruptedException, TimeoutException {
        if (!allowTopicDeletion || !name.matches(MANAGED_NAME)) {
            throw new AccessDeniedException("El topico esta protegido");
        }
        if (!name.equals(confirm)) {
            throw new BadRequestException("Confirme el nombre del topico en el parametro confirm");
        }
        esperar(admin.deleteTopics(List.of(name)).all());
    }

    private <T> T esperar(KafkaFuture<T> future) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw error;
        }
    }

    @ExceptionHandler({ExecutionException.class, InterruptedException.class, TimeoutException.class, KafkaException.class})
    public ProblemDetail kafkaError(Exception error) {
        Throwable cause = error instanceof ExecutionException ? error.getCause() : error;
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        if (cause instanceof UnknownTopicOrPartitionException) {
            status = HttpStatus.NOT_FOUND;
        } else if (cause instanceof TopicExistsException || cause instanceof TopicDeletionDisabledException) {
            status = HttpStatus.CONFLICT;
        } else if (cause instanceof InvalidTopicException || cause instanceof InvalidReplicationFactorException
            || cause instanceof InvalidPartitionsException || cause instanceof InvalidConfigurationException) {
            status = HttpStatus.BAD_REQUEST;
        }
        log.warn("Operacion administrativa Kafka fallida status={} error={}", status.value(),
            cause.getClass().getSimpleName());
        return ProblemDetail.forStatusAndDetail(status, switch (status) {
            case NOT_FOUND -> "Topico no encontrado";
            case CONFLICT -> "La operacion contradice el estado o la configuracion del topico";
            case BAD_REQUEST -> "Kafka rechazo los parametros del topico";
            default -> "Kafka no pudo confirmar la operacion";
        });
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail cuerpoInvalido() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Cuerpo JSON invalido");
    }

    public record CreateTopic(
        @NotNull @Pattern(regexp = MANAGED_NAME) String name,
        @NotNull @Min(1) @Max(12) Integer partitions,
        @NotNull @Min(1) @Max(2) Short replicationFactor,
        @NotNull @Positive Long retentionMs
    ) {
    }

    public record TopicDetails(String name, List<PartitionDetails> partitions, String retentionMs,
                               String cleanupPolicy) {
    }

    public record PartitionDetails(int partition, Integer leader, List<Integer> replicas, List<Integer> isr) {
    }
}


package com.retail.inventory.config;

import com.retail.inventory.consumer.InventarioMovimientoValidator.MovimientoInvalidoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.CommonDelegatingErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@Slf4j
public class InventarioKafkaConfig {

    public static final String TOPIC = "inventario.movimientos";
    public static final String TOPIC_ALERTAS = "inventario.alertas";

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> analiticaKafkaListenerContainerFactory(
        ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
        ConsumerFactory<Object, Object> consumerFactory,
        DefaultErrorHandler inventarioKafkaErrorHandler,
        @Value("${app.kafka.group-id}") String historialGroup,
        @Value("${app.kafka.analitica-group-id}") String analiticaGroup
    ) {
        if (analiticaGroup.isBlank() || analiticaGroup.equals(historialGroup)) {
            throw new IllegalArgumentException("Kafka requiere un grupo de analitica distinto al de historial");
        }
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, consumerFactory);
        var errorHandler = new CommonDelegatingErrorHandler(new CommonContainerStoppingErrorHandler());
        errorHandler.setErrorHandlers(Map.of(
            MovimientoInvalidoException.class, inventarioKafkaErrorHandler,
            DeserializationException.class, inventarioKafkaErrorHandler
        ));
        errorHandler.setCauseChainTraversing(true);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean(destroyMethod = "close")
    Admin kafkaAdminClient(KafkaAdmin kafkaAdmin) {
        var properties = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 4500);
        return Admin.create(properties);
    }

    @Bean
    NewTopic movimientosTopic(@Value("${app.kafka.replication-factor}") int replicas) {
        return TopicBuilder.name(TOPIC)
            .partitions(3)
            .replicas(replicas)
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
            .build();
    }

    @Bean
    NewTopic alertasTopic(@Value("${app.kafka.replication-factor}") int replicas) {
        return TopicBuilder.name(TOPIC_ALERTAS)
            .partitions(3)
            .replicas(replicas)
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
            .build();
    }

    @Bean
    DefaultErrorHandler inventarioKafkaErrorHandler() {
        var handler = new DefaultErrorHandler(
            (record, error) -> log.error(
                "Evento Kafka omitido: topic={} partition={} offset={} error={}",
                record.topic(),
                record.partition(),
                record.offset(),
                NestedExceptionUtils.getMostSpecificCause(error).getClass().getSimpleName()
            ),
            new FixedBackOff(1000L, 2L)
        );

        handler.addNotRetryableExceptions(IllegalArgumentException.class);

        return handler;
    }
}

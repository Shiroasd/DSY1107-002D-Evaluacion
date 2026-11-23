package com.retail.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.domain.MovimientoInventario;
import com.retail.inventory.domain.AlertaInventario;
import com.retail.inventory.consumer.InventarioAnaliticaConsumer;
import com.retail.inventory.dto.InventarioAlertaEvent;
import com.retail.inventory.dto.InventarioMovimientoEvent;
import com.retail.inventory.dto.ProductRequestDto;
import com.retail.inventory.repository.MovimientoInventarioRepository;
import com.retail.inventory.repository.AlertaInventarioRepository;
import com.retail.inventory.repository.MetricaInventarioRepository;
import com.retail.inventory.service.AlertaInventarioService;
import com.retail.inventory.service.AnaliticaInventarioService;
import com.retail.inventory.service.InventoryEventPublisher;
import com.retail.inventory.service.ProductService;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.retail.inventory.config.InventarioKafkaConfig.TOPIC;
import static com.retail.inventory.config.InventarioKafkaConfig.TOPIC_ALERTAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "app.kafka.enabled=true",
    "app.kafka.replication-factor=1",
    "app.kafka.allow-topic-deletion=true",
    "spring.datasource.url=jdbc:h2:mem:kafka-${random.uuid};DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@EmbeddedKafka(kraft = false)
@DirtiesContext
class InventarioKafkaIntegrationTest {
    private static final String PRODUCTS = "/api/v1/products";
    private static final String HISTORY = "/api/v1/movimientos";
    private static final String ALERTS = "/api/v1/alertas";
    private static final String ANALYTICS = "/api/v1/analytics/inventory";
    private static final String ANALYTICS_GROUP = "inventario-analitica-group";
    private static final String ADMIN_TOPICS = "/api/v1/admin/kafka/topics";
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MovimientoInventarioRepository history;
    @Autowired AlertaInventarioRepository alerts;
    @Autowired MetricaInventarioRepository metrics;
    @Autowired KafkaListenerEndpointRegistry listeners;
    @Autowired EmbeddedKafkaBroker kafka;
    @Autowired TransactionTemplate transactions;
    @Autowired ProductService products;
    @MockBean InventoryEventPublisher rabbitPublisher;
    @MockBean JwtDecoder jwtDecoder;
    @SpyBean AlertaInventarioService alertService;
    @SpyBean AnaliticaInventarioService analytics;
    @SpyBean(name = "kafkaAdminClient") Admin adminClient;

    @Test
    void crudPublishesAfterCommitAndHistoryIsQueryable() throws Exception {
        var request = product("KAFKA-DEMO", 17);
        var created = mvc.perform(post(PRODUCTS).with(jwt().authorities(ADMIN))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
            .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        transactions.executeWithoutResult(tx -> {
            products.updateProduct(id, product("KAFKA-DEMO", 3));
            products.createProduct(product("KAFKA-ROLLBACK", 2));
            tx.setRollbackOnly();
        });
        verify(rabbitPublisher, never()).publishStockModificado(argThat(e -> e.getProductId().equals(id)));
        products.updateProduct(id, product("KAFKA-DEMO", 17));
        products.updateProduct(id, product("KAFKA-DEMO", 5));

        request.setStock(3);
        mvc.perform(put(PRODUCTS + "/" + id).with(jwt().authorities(ADMIN))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
            .andExpect(status().isOk());
        verify(rabbitPublisher).publishStockModificado(argThat(e ->
            e.getProductId().equals(id) && e.getStockAnterior() == 17 && e.getStockActual() == 5));
        verify(rabbitPublisher).publishStockModificado(argThat(e ->
            e.getProductId().equals(id) && e.getStockAnterior() == 5 && e.getStockActual() == 3));

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(history.findAll()).filteredOn(e -> e.getProductoId().equals(id))
                .extracting(MovimientoInventario::getTipo)
                .containsExactlyInAnyOrder(InventarioMovimientoEvent.Tipo.CREACION,
                    InventarioMovimientoEvent.Tipo.SALIDA, InventarioMovimientoEvent.Tipo.SALIDA);
            assertThat(history.findAll()).noneMatch(e -> "KAFKA-ROLLBACK".equals(e.getSku()));
            assertThat(alerts.findAll()).filteredOn(e -> e.getProductoId().equals(id))
                .singleElement().satisfies(alert -> {
                    assertThat(alert.getStockActual()).isEqualTo(3);
                    assertThat(alert.getUmbral()).isEqualTo(3);
                    assertThat(history.existsById(alert.getEventId())).isTrue();
                });
            assertThat(alerts.findAll()).noneMatch(e -> "KAFKA-ROLLBACK".equals(e.getSku()));
        });
        mvc.perform(get(ALERTS)).andExpect(status().isUnauthorized());
        mvc.perform(get(ALERTS).with(jwt())).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.productoId == " + id + ")].stockActual").value(3));
        mvc.perform(get(HISTORY)).andExpect(status().isUnauthorized());
        mvc.perform(get(HISTORY).param("productoId", Long.toString(id)).param("size", "1").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].stockAnterior").value(5))
            .andExpect(jsonPath("$.content[0].stockNuevo").value(3))
            .andExpect(jsonPath("$.content[0].diferencia").value(-2))
            .andExpect(jsonPath("$.content[0].usuario").value("user"));
        mvc.perform(get(PRODUCTS + "/" + id + "/movimientos").param("orden", "asc").with(jwt()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].tipo").value("CREACION"));
        mvc.perform(get(HISTORY).param("size", "101").with(jwt())).andExpect(status().isBadRequest());
        mvc.perform(get(HISTORY).param("page", "abc").with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void alertsSkipInvalidRecordsAndRetryTransientPersistenceFailure() throws Exception {
        long id = 999998L;
        var first = new InventarioAlertaEvent(UUID.randomUUID(), id, "ALERT", "Producto",
            2, 3, Instant.now(), null);
        var invalid = new InventarioAlertaEvent(UUID.randomUUID(), id, "ALERT", "Producto",
            4, 3, Instant.now(), null);
        var last = new InventarioAlertaEvent(UUID.randomUUID(), id, "ALERT", "Producto",
            1, 3, Instant.now(), null);

        doThrow(new TransientDataAccessResourceException("Fallo temporal de prueba")).doCallRealMethod()
            .when(alertService).registrar(argThat(event -> event != null && event.eventId().equals(last.eventId())));

        String firstPayload = json.writeValueAsString(first);
        RecordMetadata lastRecord;
        try (var producer = new KafkaProducer<>(KafkaTestUtils.producerProps(kafka),
            new StringSerializer(), new StringSerializer())) {
            for (String payload : List.of(firstPayload, firstPayload, "{broken",
                json.writeValueAsString(invalid), json.writeValueAsString(event(id, 2)))) {
                producer.send(new ProducerRecord<>(TOPIC_ALERTAS, Long.toString(id), payload))
                    .get(10, TimeUnit.SECONDS);
            }
            lastRecord = producer.send(new ProducerRecord<>(TOPIC_ALERTAS, Long.toString(id),
                json.writeValueAsString(last))).get(10, TimeUnit.SECONDS);
        }

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(alerts.findAll()).filteredOn(e -> e.getProductoId().equals(id))
                .extracting(AlertaInventario::getEventId).containsExactlyInAnyOrder(first.eventId(), last.eventId());
            var offsets = adminClient.listConsumerGroupOffsets("inventario-alertas-group")
                .partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);
            assertThat(offsets).hasEntrySatisfying(new TopicPartition(TOPIC_ALERTAS, lastRecord.partition()),
                offset -> assertThat(offset.offset()).isGreaterThan(lastRecord.offset()));
        });
        verify(alertService, times(2)).registrar(argThat(e -> e != null && e.eventId().equals(last.eventId())));
    }

    @Test
    void kafkaAdminRequiresExplicitEntraRoleAndProtectsBusinessTopics() throws Exception {
        mvc.perform(get(ADMIN_TOPICS)).andExpect(status().isUnauthorized());
        when(jwtDecoder.decode("user-jwt")).thenReturn(Jwt.withTokenValue("user-jwt")
            .header("alg", "RS256").subject("user").claim("roles", List.of("User")).build());
        mvc.perform(get(ADMIN_TOPICS).header("Authorization", "Bearer user-jwt")).andExpect(status().isForbidden());
        mvc.perform(post(ADMIN_TOPICS).header("Authorization", "Bearer user-jwt")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        var admin = jwt().jwt(token -> token.claim("roles", List.of("Admin")));
        mvc.perform(get(ADMIN_TOPICS).with(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems(TOPIC, TOPIC_ALERTAS)));
        for (String topic : List.of(TOPIC, TOPIC_ALERTAS)) {
            mvc.perform(get(ADMIN_TOPICS + "/" + topic).with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.partitions.length()").value(3))
                .andExpect(jsonPath("$.partitions[0].replicas.length()").value(1))
                .andExpect(jsonPath("$.retentionMs").value("604800000"))
                .andExpect(jsonPath("$.cleanupPolicy").value("delete"));
            mvc.perform(delete(ADMIN_TOPICS + "/" + topic).with(admin).param("confirm", topic))
                .andExpect(status().isForbidden());
            verify(adminClient, never()).deleteTopics(List.of(topic));
        }
        String name = "demo." + UUID.randomUUID();
        String body = json.writeValueAsString(new com.retail.inventory.controller.KafkaAdminController.CreateTopic(
            name, 2, (short) 1, 86400000L));
        mvc.perform(post(ADMIN_TOPICS).with(admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(ADMIN_TOPICS).with(admin).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mvc.perform(post(ADMIN_TOPICS).with(admin).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
        mvc.perform(get(ADMIN_TOPICS + "/" + name).with(admin)).andExpect(status().isOk())
            .andExpect(jsonPath("$.partitions.length()").value(2));
        mvc.perform(delete(ADMIN_TOPICS + "/" + name).with(admin)).andExpect(status().isBadRequest());
        verify(adminClient, never()).deleteTopics(List.of(name));
        var deleted = mock(DeleteTopicsResult.class);
        when(deleted.all()).thenReturn(KafkaFuture.completedFuture(null));
        doReturn(deleted).when(adminClient).deleteTopics(List.of(name));
        mvc.perform(delete(ADMIN_TOPICS + "/" + name).with(admin).param("confirm", name))
            .andExpect(status().isNoContent());
        verify(adminClient).deleteTopics(List.of(name));
        mvc.perform(get(ADMIN_TOPICS + "/demo.unknown").with(admin)).andExpect(status().isNotFound());
    }

    @Test
    void malformedAndInvalidRecordsDoNotBlockAndDuplicateIsIgnored() throws Exception {
        long productId = 999999L;
        String key = Long.toString(productId);
        var first = event(productId, 2);
        var invalid = event(productId, 99);
        var last = event(productId, 2);
        String payload = json.writeValueAsString(first);
        try (var producer = new KafkaProducer<>(KafkaTestUtils.producerProps(kafka),
            new StringSerializer(), new StringSerializer())) {
            for (String value : new String[]{payload, payload, "{broken", json.writeValueAsString(invalid),
                json.writeValueAsString(last)}) {
                producer.send(new ProducerRecord<>(TOPIC, key, value)).get(10, TimeUnit.SECONDS);
            }
        }
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(history.findAll()).filteredOn(e -> e.getProductoId().equals(productId))
                .extracting(MovimientoInventario::getEventId)
                .containsExactlyInAnyOrder(first.eventId(), last.eventId()));
    }

    @Test
    void analyticsAndHistoryIndependentlyConsumeValidMovements() throws Exception {
        long id = 999997L;
        Instant latest = Instant.parse("2026-01-03T12:00:00Z");
        var zero = movement(id, 0, 0, InventarioMovimientoEvent.Tipo.CREACION, latest.minusSeconds(200));
        var entry = movement(id, 0, 8, InventarioMovimientoEvent.Tipo.ENTRADA, latest);
        var exit = movement(id, 8, 3, InventarioMovimientoEvent.Tipo.SALIDA, latest.minusSeconds(100));
        var otherProduct = movement(id - 1, 10, 8, InventarioMovimientoEvent.Tipo.AJUSTE, latest);
        var invalid = movement(id, 8, 3, InventarioMovimientoEvent.Tipo.ENTRADA, latest);
        RecordMetadata last;
        try (var producer = movementProducer()) {
            for (var valid : List.of(zero, zero, entry, otherProduct, invalid)) {
                sendMovement(producer, valid);
            }
            producer.send(new ProducerRecord<>(TOPIC, Long.toString(id), "{broken")).get(10, TimeUnit.SECONDS);
            last = sendMovement(producer, exit);
        }
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(history.findAll()).filteredOn(e -> e.getProductoId().equals(id))
                .extracting(MovimientoInventario::getEventId)
                .containsExactlyInAnyOrder(zero.eventId(), entry.eventId(), exit.eventId());
            assertThat(metrics.findById(id)).hasValueSatisfying(metric -> {
                assertThat(metric.getMovimientos()).isEqualTo(3);
                assertThat(metric.getEntradas()).isEqualTo(1);
                assertThat(metric.getSalidas()).isEqualTo(1);
                assertThat(metric.getUnidadesIngresadas()).isEqualTo(8);
                assertThat(metric.getUnidadesRetiradas()).isEqualTo(5);
                assertThat(metric.getUltimoMovimiento()).isEqualTo(latest);
                assertThat(metric.getUltimoProcesamiento()).isAfter(latest);
            });
            assertThat(metrics.findById(id - 1)).hasValueSatisfying(metric -> {
                assertThat(metric.getSalidas()).isEqualTo(1);
                assertThat(metric.getUnidadesRetiradas()).isEqualTo(2);
            });
            assertThat(offset(ANALYTICS_GROUP, last)).isGreaterThanOrEqualTo(last.offset() + 1);
            assertThat(offset("inventario-historial-group", last)).isGreaterThanOrEqualTo(last.offset() + 1);
        });

        for (String route : List.of("/summary", "/products", "/top-products")) {
            mvc.perform(get(ANALYTICS + route)).andExpect(status().isUnauthorized());
        }
        var summary = analytics.resumen();
        mvc.perform(get(ANALYTICS + "/summary").with(jwt())).andExpect(status().isOk())
            .andExpect(jsonPath("$.movimientos").value((int) summary.movimientos()))
            .andExpect(jsonPath("$.unidadesIngresadas").value((int) summary.unidadesIngresadas()));
        mvc.perform(get(ANALYTICS + "/products").with(jwt()).param("size", "100"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[?(@.productoId == " + id + ")].movimientos").value(3));
        mvc.perform(get(ANALYTICS + "/top-products").with(jwt()).param("limit", "5"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.productoId == " + id + ")].movimientos").value(3));
        mvc.perform(get(ANALYTICS + "/top-products").with(jwt()).param("limit", "101"))
            .andExpect(status().isBadRequest());
        mvc.perform(get(ANALYTICS + "/products").with(jwt()).param("page", "abc"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void analyticsRollsBackAtomicallyAndResumesFromUncommittedOffset() throws Exception {
        long id = 999995L;
        var first = movement(id, 0, 2, InventarioMovimientoEvent.Tipo.CREACION, Instant.now());
        var second = movement(id, 2, 5, InventarioMovimientoEvent.Tipo.ENTRADA, Instant.now());
        transactions.executeWithoutResult(tx -> {
            assertThat(analytics.procesar(first)).isTrue();
            tx.setRollbackOnly();
        });
        assertThat(metrics.findById(id)).isEmpty();

        var unavailable = new TransientDataAccessResourceException("Fallo de persistencia de prueba");
        doThrow(unavailable, unavailable, unavailable).doCallRealMethod().when(analytics)
            .procesar(argThat(event -> event != null && second.eventId().equals(event.eventId())));
        var listener = listeners.getListenerContainer(InventarioAnaliticaConsumer.LISTENER_ID);
        try (var producer = movementProducer()) {
            var committed = sendMovement(producer, first);
            await().atMost(20, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(offset(ANALYTICS_GROUP, committed)).isEqualTo(committed.offset() + 1));
            var failed = sendMovement(producer, second);
            await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(listener.isRunning()).isFalse();
                assertThat(history.existsById(second.eventId())).isTrue();
                assertThat(metrics.findById(id).orElseThrow().getMovimientos()).isEqualTo(1);
                assertThat(offset(ANALYTICS_GROUP, failed)).isEqualTo(failed.offset());
            });
            listener.start();
            await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
                assertThat(offset(ANALYTICS_GROUP, failed)).isEqualTo(failed.offset() + 1);
                assertThat(metrics.findById(id).orElseThrow().getUnidadesIngresadas()).isEqualTo(5);
            });
        } finally {
            if (!listener.isRunning()) {
                listener.start();
            }
        }
    }

    private long offset(String group, RecordMetadata record) throws Exception {
        var offsets = adminClient.listConsumerGroupOffsets(group)
            .partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);
        var offset = offsets.get(new TopicPartition(record.topic(), record.partition()));
        return offset == null ? -1 : offset.offset();
    }

    private KafkaProducer<String, String> movementProducer() {
        return new KafkaProducer<>(KafkaTestUtils.producerProps(kafka), new StringSerializer(), new StringSerializer());
    }

    private RecordMetadata sendMovement(KafkaProducer<String, String> producer, InventarioMovimientoEvent event)
        throws Exception {
        return producer.send(new ProducerRecord<>(TOPIC, event.productoId().toString(),
            json.writeValueAsString(event))).get(10, TimeUnit.SECONDS);
    }

    private InventarioMovimientoEvent movement(long id, int before, int after,
                                                InventarioMovimientoEvent.Tipo type, Instant date) {
        return new InventarioMovimientoEvent(UUID.randomUUID(), id, "SKU-" + id, "Producto de prueba",
            before, after, after - before, type, date, null);
    }

    private InventarioMovimientoEvent event(long productId, int difference) {
        return new InventarioMovimientoEvent(UUID.randomUUID(), productId, null, "Producto de prueba",
            0, 2, difference, InventarioMovimientoEvent.Tipo.CREACION, Instant.now(), null);
    }

    private ProductRequestDto product(String sku, int stock) {
        return ProductRequestDto.builder().sku(sku).name("Producto de prueba")
            .categoryId(1L).price(BigDecimal.TEN).stock(stock).build();
    }
}

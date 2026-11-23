package com.retail.inventory;

import com.rabbitmq.client.Channel;
import com.retail.inventory.consumer.MessageAckHandler;
import com.retail.inventory.consumer.NotificacionesConsumer;
import com.retail.inventory.consumer.ReposicionConsumer;
import com.retail.inventory.dto.ProductRequestDto;
import com.retail.inventory.repository.AlertaInventarioRepository;
import com.retail.inventory.repository.MetricaInventarioRepository;
import com.retail.inventory.repository.MovimientoInventarioRepository;
import com.retail.inventory.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@EnabledIfEnvironmentVariable(named = "CLOUDSTOCK_ISOLATED_RABBIT_TEST", matches = "true")
@SpringBootTest(properties = {
    "app.kafka.enabled=true",
    "app.kafka.replication-factor=1",
    "spring.datasource.url=jdbc:h2:mem:mensajeria-${random.uuid};DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=always",
    "spring.rabbitmq.listener.simple.auto-startup=true"
})
@EmbeddedKafka(kraft = false)
@DirtiesContext
class InventarioMensajeriaAisladaTest {
    @Autowired ProductService products;
    @Autowired AlertaInventarioRepository alerts;
    @Autowired MovimientoInventarioRepository history;
    @Autowired MetricaInventarioRepository metrics;
    @SpyBean NotificacionesConsumer notifications;
    @SpyBean ReposicionConsumer restock;
    @SpyBean MessageAckHandler acknowledgments;

    @Test
    void committedStockChangeReachesKafkaAndRabbitMq() {
        var request = ProductRequestDto.builder().sku("BROKER-E2E").name("Prueba aislada")
            .categoryId(1L).price(BigDecimal.TEN).stock(5).build();
        long id = products.createProduct(request).getId();
        request.setStock(3);
        products.updateProduct(id, request);

        await().atMost(Duration.ofSeconds(25)).untilAsserted(() -> {
            assertThat(alerts.findAll()).singleElement().satisfies(alert -> {
                assertThat(alert.getProductoId()).isEqualTo(id);
                assertThat(alert.getStockActual()).isEqualTo(3);
                assertThat(history.existsById(alert.getEventId())).isTrue();
            });
            assertThat(metrics.findById(id).orElseThrow().getMovimientos()).isEqualTo(2);
            verify(notifications).procesarNotificacionStock(argThat(e -> e != null
                && e.getProductId().equals(id) && e.getStockAnterior() == 5 && e.getStockActual() == 3),
                any(Channel.class), anyLong(), eq(false));
            verify(restock).generarSolicitudReposicion(argThat(e -> e != null && e.getProductId().equals(id)
                && e.getStockActual() == 3), any(Channel.class), anyLong(), eq(false));
            verify(acknowledgments).ack(any(Channel.class), anyLong(), eq("ms-notificaciones"));
            verify(acknowledgments).ack(any(Channel.class), anyLong(), eq("ms-reposicion"));
        });
    }
}

package com.retail.inventory.service;

import com.retail.inventory.dto.admin.CreateBindingRequestDto;
import com.retail.inventory.dto.admin.CreateExchangeRequestDto;
import com.retail.inventory.dto.admin.CreateQueueRequestDto;
import com.retail.inventory.exception.BadRequestException;
import com.retail.inventory.exception.ResourceNotFoundException;
import com.retail.inventory.service.impl.RabbitAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    private RabbitAdminService rabbitAdminService;

    @BeforeEach
    void setUp() {
        rabbitAdminService = new RabbitAdminServiceImpl(amqpAdmin);
    }

    @Test
    @DisplayName("Debe declarar una cola durable con política DLQ correctamente")
    void createQueueWithDlxShouldSucceed() {
        CreateQueueRequestDto dto = CreateQueueRequestDto.builder()
                .name("test-queue")
                .durable(true)
                .deadLetterExchange("inventory.dlx.exchange")
                .deadLetterRoutingKey("inventory.dlq")
                .build();

        when(amqpAdmin.declareQueue(any(Queue.class))).thenReturn("test-queue");

        String result = rabbitAdminService.createQueue(dto);

        assertEquals("test-queue", result);
        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin, times(1)).declareQueue(captor.capture());

        Queue captured = captor.getValue();
        assertEquals("test-queue", captured.getName());
        assertTrue(captured.isDurable());
        assertEquals("inventory.dlx.exchange", captured.getArguments().get("x-dead-letter-exchange"));
        assertEquals("inventory.dlq", captured.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    @DisplayName("Debe eliminar una cola existente retornando true")
    void deleteQueueShouldSucceed() {
        when(amqpAdmin.deleteQueue("old-queue")).thenReturn(true);

        boolean deleted = rabbitAdminService.deleteQueue("old-queue");

        assertTrue(deleted);
        verify(amqpAdmin, times(1)).deleteQueue("old-queue");
    }

    @Test
    @DisplayName("Debe purgar una cola existente")
    void purgeQueueShouldSucceed() {
        rabbitAdminService.purgeQueue("purge-target");
        verify(amqpAdmin, times(1)).purgeQueue("purge-target", false);
    }

    @Test
    @DisplayName("Debe declarar exchanges de tipo topic y direct")
    void createExchangeTypesShouldSucceed() {
        CreateExchangeRequestDto topicDto = CreateExchangeRequestDto.builder()
                .name("events.topic")
                .type("topic")
                .durable(true)
                .build();

        rabbitAdminService.createExchange(topicDto);

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin, times(1)).declareExchange(captor.capture());
        assertEquals("events.topic", captor.getValue().getName());
        assertEquals("topic", captor.getValue().getType());
    }

    @Test
    @DisplayName("Debe rechazar un tipo de exchange no soportado")
    void createExchangeInvalidTypeShouldThrowBadRequest() {
        CreateExchangeRequestDto invalidDto = CreateExchangeRequestDto.builder()
                .name("invalid.exchange")
                .type("invalid-type")
                .build();

        assertThrows(BadRequestException.class, () -> rabbitAdminService.createExchange(invalidDto));
    }

    @Test
    @DisplayName("Debe crear y eliminar un binding correctamente")
    void createAndDeleteBindingShouldSucceed() {
        CreateBindingRequestDto dto = CreateBindingRequestDto.builder()
                .queueName("queue-a")
                .exchangeName("exchange-a")
                .routingKey("key.test")
                .build();

        rabbitAdminService.createBinding(dto);
        verify(amqpAdmin, times(1)).declareBinding(any(Binding.class));

        rabbitAdminService.deleteBinding(dto);
        verify(amqpAdmin, times(1)).removeBinding(any(Binding.class));
    }

    @Test
    @DisplayName("Debe retornar métricas de cola cuando existen en el broker")
    void getQueueInfoShouldReturnProperties() {
        Properties properties = new Properties();
        properties.setProperty("QUEUE_MESSAGE_COUNT", "42");
        properties.setProperty("QUEUE_CONSUMER_COUNT", "2");

        when(amqpAdmin.getQueueProperties("active-queue")).thenReturn(properties);

        Map<String, Object> info = rabbitAdminService.getQueueInfo("active-queue");

        assertNotNull(info);
        assertEquals("42", info.get("QUEUE_MESSAGE_COUNT"));
        assertEquals("2", info.get("QUEUE_CONSUMER_COUNT"));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la cola no existe")
    void getQueueInfoNotFoundShouldThrowException() {
        when(amqpAdmin.getQueueProperties("ghost-queue")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> rabbitAdminService.getQueueInfo("ghost-queue"));
    }
}

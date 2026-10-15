package com.retail.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.dto.BindingRequestDto;
import com.retail.inventory.dto.ExchangeRequestDto;
import com.retail.inventory.dto.QueueRequestDto;
import com.retail.inventory.service.RabbitAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RabbitAdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RabbitAdminService rabbitAdminService;

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/queues sin token debe retornar 401 Unauthorized")
    void createQueueWithoutTokenShouldReturn401() throws Exception {
        QueueRequestDto dto = QueueRequestDto.builder()
                .name("test.queue")
                .durable(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/queues con rol User (vendedor) debe retornar 403 Forbidden")
    void createQueueWithUserRoleShouldReturn403() throws Exception {
        QueueRequestDto dto = QueueRequestDto.builder()
                .name("test.queue")
                .durable(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/queues")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "seller-user-01"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/queues con rol Admin debe permitir crear la cola y retornar 201 Created")
    void createQueueWithAdminRoleShouldReturn201() throws Exception {
        QueueRequestDto dto = QueueRequestDto.builder()
                .name("pedidos.nuevos.queue")
                .durable(true)
                .build();

        when(rabbitAdminService.createQueue(any())).thenReturn(Map.of(
                "status", "SUCCESS",
                "queueName", "pedidos.nuevos.queue"
        ));

        mockMvc.perform(post("/api/v1/admin/rabbitmq/queues")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user-01"))
                                .authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.queueName").value("pedidos.nuevos.queue"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/queues con nombre inválido debe retornar 400 Bad Request por validación")
    void createQueueWithInvalidNameShouldReturn400() throws Exception {
        QueueRequestDto dto = QueueRequestDto.builder()
                .name("cola con espacios y @#$%")
                .durable(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/queues")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user-01"))
                                .authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/rabbitmq/queues/{name} sin rol Admin debe retornar 403 Forbidden")
    void deleteQueueWithoutAdminRoleShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/rabbitmq/queues/test.queue")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "seller-user-01"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/exchanges sin rol Admin debe retornar 403 Forbidden")
    void createExchangeWithoutAdminRoleShouldReturn403() throws Exception {
        ExchangeRequestDto dto = ExchangeRequestDto.builder()
                .name("ventas.exchange")
                .type("TOPIC")
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/exchanges")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "seller-user-01"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/bindings sin rol Admin debe retornar 403 Forbidden")
    void createBindingWithoutAdminRoleShouldReturn403() throws Exception {
        BindingRequestDto dto = BindingRequestDto.builder()
                .destination("ventas.queue")
                .destinationType("QUEUE")
                .exchange("ventas.exchange")
                .routingKey("ventas.#")
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/bindings")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "seller-user-01"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}

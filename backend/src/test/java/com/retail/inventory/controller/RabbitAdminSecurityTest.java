package com.retail.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.dto.admin.CreateBindingRequestDto;
import com.retail.inventory.dto.admin.CreateExchangeRequestDto;
import com.retail.inventory.dto.admin.CreateQueueRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("GET /api/v1/admin/rabbitmq/queues/test-queue sin token debe retornar 401 Unauthorized")
    void getQueueWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/rabbitmq/queues/test-queue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/queues con usuario sin rol Admin debe retornar 403 Forbidden")
    void createQueueWithoutAdminRoleShouldReturn403() throws Exception {
        CreateQueueRequestDto dto = CreateQueueRequestDto.builder()
                .name("test-queue")
                .durable(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/queues")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-seller"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/queues con rol Admin debe retornar 201 Created")
    void createQueueWithAdminRoleShouldReturn201() throws Exception {
        CreateQueueRequestDto dto = CreateQueueRequestDto.builder()
                .name("stock-alert-custom")
                .durable(true)
                .build();

        when(rabbitAdminService.createQueue(any(CreateQueueRequestDto.class))).thenReturn("stock-alert-custom");

        mockMvc.perform(post("/api/v1/admin/rabbitmq/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queue").value("stock-alert-custom"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/admin/rabbitmq/queues/{queueName} con rol Admin debe retornar 200 OK")
    void getQueueInfoWithAdminRoleShouldReturn200() throws Exception {
        when(rabbitAdminService.getQueueInfo("audit-log"))
                .thenReturn(Map.of("QUEUE_MESSAGE_COUNT", "15", "QUEUE_CONSUMER_COUNT", "1"));

        mockMvc.perform(get("/api/v1/admin/rabbitmq/queues/audit-log")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.QUEUE_MESSAGE_COUNT").value("15"))
                .andExpect(jsonPath("$.QUEUE_CONSUMER_COUNT").value("1"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/rabbitmq/queues/{queueName} con rol Admin debe retornar 204 No Content")
    void deleteQueueWithAdminRoleShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/rabbitmq/queues/temp-queue")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/exchanges con rol Admin debe retornar 201 Created")
    void createExchangeWithAdminRoleShouldReturn201() throws Exception {
        CreateExchangeRequestDto dto = CreateExchangeRequestDto.builder()
                .name("custom.topic.exchange")
                .type("topic")
                .durable(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/exchanges")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exchange").value("custom.topic.exchange"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/bindings con rol Admin debe retornar 201 Created")
    void createBindingWithAdminRoleShouldReturn201() throws Exception {
        CreateBindingRequestDto dto = CreateBindingRequestDto.builder()
                .queueName("stock-alert")
                .exchangeName("inventory.topic.exchange")
                .routingKey("product.stock.low")
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/bindings")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routingKey").value("product.stock.low"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/rabbitmq/queues con nombre inválido debe retornar 400 Bad Request")
    void createQueueWithInvalidNameShouldReturn400() throws Exception {
        CreateQueueRequestDto dto = CreateQueueRequestDto.builder()
                .name("invalid queue name with spaces!")
                .build();

        mockMvc.perform(post("/api/v1/admin/rabbitmq/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}

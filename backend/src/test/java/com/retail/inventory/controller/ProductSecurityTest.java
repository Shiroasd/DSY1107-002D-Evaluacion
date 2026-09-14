package com.retail.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.dto.ProductRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/products sin token debe retornar 401 Unauthorized")
    void getProductsWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/v1/products con token autenticado debe retornar 200 OK")
    void getProductsWithAuthenticatedUserShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-seller-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/products sin rol Admin debe retornar 403 Forbidden")
    void createProductWithoutAdminRoleShouldReturn403() throws Exception {
        ProductRequestDto newProduct = ProductRequestDto.builder()
                .sku("TEST-999")
                .name("Producto de Prueba")
                .price(new BigDecimal("29.99"))
                .stock(10)
                .categoryId(1L)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-seller-123"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("POST /api/v1/products con rol Admin debe permitir creación y retornar 201 Created")
    void createProductWithAdminRoleShouldReturn201() throws Exception {
        ProductRequestDto newProduct = ProductRequestDto.builder()
                .sku("POS-TEST-NEW")
                .name("Terminal POS de Prueba")
                .price(new BigDecimal("199.99"))
                .stock(15)
                .categoryId(1L)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user-001"))
                                .authorities(new SimpleGrantedAuthority("ROLE_Admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("POS-TEST-NEW"))
                .andExpect(jsonPath("$.name").value("Terminal POS de Prueba"));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} sin rol Admin debe retornar 403 Forbidden")
    void deleteProductWithoutAdminRoleShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-seller-123"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/products con scope delegado OT.Create debe permitir creación y retornar 201 Created")
    void createProductWithScopeOTCreateShouldReturn201() throws Exception {
        ProductRequestDto newProduct = ProductRequestDto.builder()
                .sku("SCOPE-OT-001")
                .name("Terminal OT Autorizado")
                .price(new BigDecimal("150.00"))
                .stock(5)
                .categoryId(1L)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "api-client-ot"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_OT.Create")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SCOPE-OT-001"))
                .andExpect(jsonPath("$.name").value("Terminal OT Autorizado"));
    }

    @Test
    @DisplayName("CORS Preflight OPTIONS /api/v1/products desde origen https://32.193.45.223 debe retornar 200 OK con cabeceras CORS")
    void preflightCorsFromAwsFrontendShouldReturn200() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", "https://32.193.45.223")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://32.193.45.223"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
    }

    @Test
    @DisplayName("Petición autenticada desde origen AWS https://32.193.45.223 debe incluir cabecera Access-Control-Allow-Origin")
    void actualRequestFromAwsFrontendShouldIncludeCorsHeader() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Origin", "https://32.193.45.223")
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "user-aws-frontend"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://32.193.45.223"));
    }
}

package com.retail.inventory.config;

import com.retail.inventory.exception.CustomAccessDeniedHandler;
import com.retail.inventory.exception.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AzureJwtGrantedAuthoritiesConverter azureJwtGrantedAuthoritiesConverter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Value("${spring.cloud.azure.active-directory.credential.client-id:afcd0a4e-f3f3-4934-9861-4d8d13cecc30}")
    private String clientId;

    @Value("${spring.cloud.azure.active-directory.app-id-uri:api://afcd0a4e-f3f3-4934-9861-4d8d13cecc30}")
    private String appIdUri;

    @Value("${spring.cloud.azure.active-directory.profile.tenant-id:73d72038-30bf-4ab9-85bc-a402de679470}")
    private String tenantId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Habilitar CORS para integración con Angular SPA en AWS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Deshabilitar CSRF dado que el Resource Server es Stateless con Bearer Tokens
            .csrf(AbstractHttpConfigurer::disable)
            
            // Permitir iframes para consola H2 en desarrollo
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            
            // Política de sesión Stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configuración de autorización de endpoints
            .authorizeHttpRequests(auth -> auth
                // Consola H2 para inspección de datos locales
                .requestMatchers("/h2-console/**").permitAll()
                // Peticiones Preflight de CORS siempre permitidas
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Endpoints GET accesibles para cualquier usuario autenticado
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").authenticated()
                // Endpoints de creación y modificación: Validan rol Admin o permisos/scopes delegados (ej. OT.Create)
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**", "/api/v1/categories/**")
                    .hasAnyAuthority("ROLE_Admin", "SCOPE_OT.Create", "OT.Create")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**", "/api/v1/categories/**")
                    .hasAnyAuthority("ROLE_Admin", "SCOPE_OT.Create", "OT.Create", "SCOPE_OT.Update", "OT.Update")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**", "/api/v1/categories/**")
                    .hasAnyAuthority("ROLE_Admin", "SCOPE_OT.Delete", "OT.Delete")
                // Cualquier otra solicitud requiere autenticación
                .anyRequest().authenticated()
            )
            
            // Manejo de excepciones en la cadena de filtros de seguridad
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            
            // Configuración de OAuth2 Resource Server con validación JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(azureJwtGrantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Endpoint JWKS unificado de Microsoft para resolución de llaves públicas de firma
        String jwkSetUri = "https://login.microsoftonline.com/common/discovery/v2.0/keys";
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // 1. Validador de vigencia de tiempo (exp y nbf)
        OAuth2TokenValidator<Jwt> timestampValidator = new org.springframework.security.oauth2.jwt.JwtTimestampValidator();

        // 2. Validador de Emisor (acepta formato v2.0 y v1.0 de Microsoft Entra ID para el tenant configurado)
        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            if (token.getIssuer() != null) {
                String iss = token.getIssuer().toString();
                if (iss.contains(tenantId) ||
                    iss.startsWith("https://login.microsoftonline.com/") ||
                    iss.startsWith("https://sts.windows.net/")) {
                    return OAuth2TokenValidatorResult.success();
                }
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emisor (iss) no reconocido: " + token.getIssuer(), null));
        };

        // 3. Validador de Audiencia (Backend App ID, App ID URI, Frontend Client ID)
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audiences = token.getAudience();
            if (audiences != null && !audiences.isEmpty()) {
                for (String aud : audiences) {
                    if (aud.contains(clientId) ||
                        aud.contains(appIdUri) ||
                        aud.contains("57c1db2d-484b-463a-b993-45c3ef349e3c") ||
                        aud.contains("00000003-0000-0000-c000-000000000000") ||
                        aud.contains("graph.microsoft.com")) {
                        return OAuth2TokenValidatorResult.success();
                    }
                }
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiencia (aud) no válida: " + audiences, null));
        };

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator, issuerValidator, audienceValidator));
        return jwtDecoder;
    }

    /**
     * Configuración de CORS requerida para permitir la comunicación entre el Frontend Angular
     * en AWS (https://32.193.45.223/) y el Resource Server (Spring Boot).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configura Access-Control-Allow-Origin para permitir exclusivamente las peticiones desde el frontend: https://32.193.45.223/
        configuration.setAllowedOrigins(Arrays.asList(
            "https://32.193.45.223",
            "https://32.193.45.223/",
            "http://32.193.45.223",
            "http://32.193.45.223/",
            "http://localhost:4200",
            "http://localhost:4200/",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:4200/"
        ));
        
        // En Access-Control-Allow-Methods, habilita los verbos HTTP necesarios (GET, POST, PUT, DELETE) y obligatoriamente OPTIONS para Preflight
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // En Access-Control-Allow-Headers, permite de forma explícita los encabezados Content-Type y Authorization
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

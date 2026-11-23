package com.retail.inventory.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CloudStockAudienceTest {
    private static final String CLIENT_ID = "cloudstock-id";
    private static final String APP_ID_URI = "api://cloudstock-id";
    private final org.springframework.security.oauth2.core.OAuth2TokenValidator<Jwt> validator = SecurityConfig.cloudStockAudience(CLIENT_ID, APP_ID_URI);

    @Test
    void acceptsOnlyExactCloudStockAudiences() {
        assertThat(validator.validate(jwt(List.of(CLIENT_ID))).hasErrors()).isFalse();
        assertThat(validator.validate(jwt(List.of(APP_ID_URI))).hasErrors()).isFalse();
        assertThat(validator.validate(jwt(List.of(CLIENT_ID, APP_ID_URI))).hasErrors()).isFalse();

        for (var audiences : List.of(
                List.<String>of(),
                List.of("other-resource"),
                List.of("00000003-0000-0000-c000-000000000000"),
                List.of("https://graph.microsoft.com"),
                List.of("api://cloudstock-id/other"),
                List.of(CLIENT_ID, "other-resource"))) {
            assertThat(validator.validate(jwt(audiences)).hasErrors()).as("aud=%s", audiences).isTrue();
        }

        var missing = Jwt.withTokenValue("test").header("alg", "RS256").subject("user").build();
        assertThat(validator.validate(missing).hasErrors()).isTrue();
    }

    private Jwt jwt(List<String> audiences) {
        return Jwt.withTokenValue("test").header("alg", "RS256").subject("user").audience(audiences).build();
    }
}

package com.digiteen.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("digiteen-wallet-service");

    @Test
    void acceptsExpectedAudience() {
        assertThat(validator.validate(jwt(List.of("digiteen-wallet-service"))).hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenIssuedForAnotherService() {
        assertThat(validator.validate(jwt(List.of("another-service"))).hasErrors()).isTrue();
    }

    private Jwt jwt(List<String> audience) {
        Instant now = Instant.now();
        return new Jwt("token", now, now.plusSeconds(60), Map.of("alg", "RS256"), Map.of(
                "sub", "user-id",
                "aud", audience));
    }
}

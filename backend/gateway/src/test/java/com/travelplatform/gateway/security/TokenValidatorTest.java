package com.travelplatform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@DisplayName("TokenValidator")
class TokenValidatorTest {

    @Inject TokenValidator tokenValidator;

    private String validToken() {
        return Jwt.issuer("travel-platform-identity")
                .subject(UUID.randomUUID().toString())
                .groups("USER")
                .sign("privateKey.pem");
    }

    @Test
    void noAuthorizationHeaderIsValid() {
        assertThat(tokenValidator.isValid(null)).isTrue();
    }

    @Test
    void aBlankAuthorizationHeaderIsValid() {
        assertThat(tokenValidator.isValid("")).isTrue();
    }

    @Test
    void aWellFormedSignedTokenIsValid() {
        assertThat(tokenValidator.isValid("Bearer " + validToken())).isTrue();
    }

    @Test
    void aTokenWithoutTheBearerPrefixIsInvalid() {
        assertThat(tokenValidator.isValid(validToken())).isFalse();
    }

    @Test
    void aMalformedTokenIsInvalid() {
        assertThat(tokenValidator.isValid("Bearer not-a-jwt-at-all")).isFalse();
    }

    @Test
    void anExpiredTokenIsInvalid() {
        var expired =
                Jwt.issuer("travel-platform-identity")
                        .subject(UUID.randomUUID().toString())
                        .expiresIn(Duration.ofSeconds(-60))
                        .sign("privateKey.pem");
        assertThat(tokenValidator.isValid("Bearer " + expired)).isFalse();
    }
}

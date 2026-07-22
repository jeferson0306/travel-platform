package com.travelplatform.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.identity.application.port.out.TokenIssuer;
import com.travelplatform.identity.domain.user.Email;
import com.travelplatform.identity.domain.user.HashedPassword;
import com.travelplatform.identity.domain.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every other service trusts the shape of the token this issues (RS256, verified with
 * identity-service's public key - see docs/adr/0006-rbac-roles.md), so its claims are asserted
 * directly here rather than only indirectly through AuthResourceTest, which never inspects the
 * token's contents. Decodes the JWT payload segment by hand (no signature verification needed -
 * this test only cares what JwtTokenIssuer, running in-process, put in the token).
 */
@QuarkusTest
@DisplayName("JwtTokenIssuer")
class JwtTokenIssuerTest {

    @Inject TokenIssuer tokenIssuer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    private Map<String, Object> claimsOf(String jwt) throws Exception {
        var payloadSegment = jwt.split("\\.")[1];
        var payloadJson =
                new String(Base64.getUrlDecoder().decode(payloadSegment), StandardCharsets.UTF_8);
        return objectMapper.readValue(payloadJson, Map.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("issues a token carrying the user's id, email and role")
    void issuesATokenWithTheExpectedClaims() throws Exception {
        var user = User.register(new Email("traveler@example.com"), new HashedPassword("hashed"));

        var issued = tokenIssuer.issue(user);
        var claims = claimsOf(issued.value());

        assertThat(claims.get("iss")).isEqualTo("travel-platform-identity");
        assertThat(claims.get("sub")).isEqualTo(user.id().value().toString());
        assertThat(claims.get("upn")).isEqualTo("traveler@example.com");
        assertThat((java.util.List<String>) claims.get("groups")).containsExactly("USER");
    }

    @Test
    @DisplayName("reports the configured TTL both on the token and the returned expiry")
    void reportsTheConfiguredTtl() {
        var user = User.register(new Email("traveler@example.com"), new HashedPassword("hashed"));

        var issued = tokenIssuer.issue(user);

        assertThat(issued.expiresInSeconds()).isEqualTo(3600);
    }
}

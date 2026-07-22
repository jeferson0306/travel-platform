package com.travelplatform.identity.infrastructure.security;

import com.travelplatform.identity.application.port.out.TokenIssuer;
import com.travelplatform.identity.domain.user.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Signs with the RSA private key configured via {@code smallrye.jwt.sign.key.location}
 * (application.yml) - RS256, not a shared secret, so only identity-service ever holds signing
 * material. Every other service verifies with the public key alone. See
 * docs/adr/0006-rbac-roles.md.
 */
@ApplicationScoped
public class JwtTokenIssuer implements TokenIssuer {

    private final String issuer;
    private final Duration ttl;

    public JwtTokenIssuer(
            @ConfigProperty(name = "identity.jwt.issuer") String issuer,
            @ConfigProperty(name = "identity.jwt.ttl-seconds", defaultValue = "3600")
                    long ttlSeconds) {
        this.issuer = issuer;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public IssuedToken issue(User user) {
        var token =
                Jwt.issuer(issuer)
                        .subject(user.id().value().toString())
                        .upn(user.email().value())
                        .groups(user.role().name())
                        .expiresIn(ttl)
                        .sign();
        return new IssuedToken(token, ttl.toSeconds());
    }
}

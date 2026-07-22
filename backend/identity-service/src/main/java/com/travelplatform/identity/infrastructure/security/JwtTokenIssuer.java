package com.travelplatform.identity.infrastructure.security;

import com.travelplatform.identity.application.port.out.TokenIssuer;
import com.travelplatform.identity.domain.user.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JwtTokenIssuer implements TokenIssuer {

    private final String issuer;
    private final String secret;
    private final Duration ttl;

    public JwtTokenIssuer(
            @ConfigProperty(name = "identity.jwt.issuer") String issuer,
            @ConfigProperty(name = "identity.jwt.secret") String secret,
            @ConfigProperty(name = "identity.jwt.ttl-seconds", defaultValue = "3600")
                    long ttlSeconds) {
        this.issuer = issuer;
        this.secret = secret;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public IssuedToken issue(User user) {
        var token =
                Jwt.issuer(issuer)
                        .subject(user.id().value().toString())
                        .upn(user.email().value())
                        .groups("traveler")
                        .expiresIn(ttl)
                        .jws()
                        .signWithSecret(secret);
        return new IssuedToken(token, ttl.toSeconds());
    }
}

package com.travelplatform.gateway.security;

import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Fast-fails a request carrying a malformed, unsigned, or expired JWT before it ever reaches a
 * backend - cheap protection against wasted upstream work. Deliberately does <b>not</b> replicate
 * role-based authorization: every backend already enforces its own {@code @RolesAllowed} rules (ADR
 * 0006), which stays the single source of truth for "who can do what." Duplicating that logic here
 * would create two places that can drift out of sync - see docs/adr/0013-api-gateway.md. A request
 * with no {@code Authorization} header at all is not rejected here either: whether an endpoint
 * requires auth is a per-backend decision this gateway does not need to know.
 */
@ApplicationScoped
public class TokenValidator {

    private static final Logger LOG = Logger.getLogger(TokenValidator.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTParser jwtParser;

    public TokenValidator(JWTParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    /**
     * Returns {@code true} if the header is absent, or present and a validly-signed, unexpired JWT.
     * Returns {@code false} only for a present-but-invalid token.
     */
    public boolean isValid(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return true;
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            return false;
        }
        var token = authorizationHeader.substring(BEARER_PREFIX.length());
        try {
            jwtParser.parse(token);
            return true;
        } catch (Exception e) {
            LOG.debug("Rejected an invalid bearer token at the gateway", e);
            return false;
        }
    }
}

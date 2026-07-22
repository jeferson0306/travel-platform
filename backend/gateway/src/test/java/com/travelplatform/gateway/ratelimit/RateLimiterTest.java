package com.travelplatform.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A tiny {@code requests-per-minute} override (see {@link LowLimitProfile}) lets this test trip the
 * limit in a handful of calls instead of 120+. Each test uses its own random {@code clientKey} -
 * Redis-backed state persists for the whole window, so sharing a key across test methods would make
 * them interfere with each other.
 */
@QuarkusTest
@TestProfile(RateLimiterTest.LowLimitProfile.class)
@DisplayName("RateLimiter")
class RateLimiterTest {

    @Inject RateLimiter rateLimiter;

    @Test
    @DisplayName("allows requests up to the configured limit, then denies the next one")
    void deniesOnceTheLimitIsExceeded() {
        var clientKey = UUID.randomUUID().toString();

        assertThat(rateLimiter.allow(clientKey).await().indefinitely()).isTrue();
        assertThat(rateLimiter.allow(clientKey).await().indefinitely()).isTrue();
        assertThat(rateLimiter.allow(clientKey).await().indefinitely()).isTrue();
        assertThat(rateLimiter.allow(clientKey).await().indefinitely()).isFalse();
    }

    @Test
    @DisplayName("different clients each get their own independent quota")
    void eachClientHasAnIndependentQuota() {
        var clientA = UUID.randomUUID().toString();
        var clientB = UUID.randomUUID().toString();

        rateLimiter.allow(clientA).await().indefinitely();
        rateLimiter.allow(clientA).await().indefinitely();
        rateLimiter.allow(clientA).await().indefinitely();

        assertThat(rateLimiter.allow(clientB).await().indefinitely())
                .as("a fresh client should not inherit another client's exhausted quota")
                .isTrue();
    }

    public static class LowLimitProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("gateway.rate-limit.requests-per-minute", "3");
        }
    }
}

package com.travelplatform.gateway.ratelimit;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Fixed-window rate limit, backed by Redis so it holds across every gateway instance rather than
 * per-process memory (a real concern once this ever runs behind more than one replica). Keyed by
 * client IP and the current one-minute window - a burst that straddles a window boundary can
 * momentarily allow up to 2x the limit, a known, accepted trade-off of fixed windows over a sliding
 * window/token bucket; simplicity was chosen over precision for this milestone.
 */
@ApplicationScoped
public class RateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ReactiveRedisDataSource redisDataSource;
    private final ReactiveValueCommands<String, Long> counters;
    private final int limitPerWindow;

    public RateLimiter(
            ReactiveRedisDataSource redisDataSource,
            @ConfigProperty(name = "gateway.rate-limit.requests-per-minute", defaultValue = "120")
                    int limitPerWindow) {
        this.redisDataSource = redisDataSource;
        this.counters = redisDataSource.value(Long.class);
        this.limitPerWindow = limitPerWindow;
    }

    /**
     * Returns {@code true} if the request is allowed, incrementing the client's counter as a side
     * effect. Returns {@code false} once the client has exceeded the limit for this window.
     */
    public Uni<Boolean> allow(String clientKey) {
        var windowKey =
                "ratelimit:"
                        + clientKey
                        + ":"
                        + (Instant.now().getEpochSecond() / WINDOW.getSeconds());
        return counters.incrby(windowKey, 1)
                .call(
                        count ->
                                count == 1
                                        ? redisDataSource.key().expire(windowKey, WINDOW)
                                        : Uni.createFrom().voidItem())
                .map(count -> count <= limitPerWindow);
    }
}

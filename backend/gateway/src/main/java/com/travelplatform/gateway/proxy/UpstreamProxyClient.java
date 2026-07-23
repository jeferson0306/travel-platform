package com.travelplatform.gateway.proxy;

import io.smallrye.faulttolerance.api.Guard;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps each upstream call in a {@link Guard} - the programmatic (not annotation-based) SmallRye
 * Fault Tolerance API. Annotations were tried first and rejected:
 * {@code @Timeout}/{@code @CircuitBreaker}/{@code @Bulkhead}/{@code @Retry} apply per
 * <em>method</em>, not per method argument, so a single {@code forward(segment, ...)} method would
 * share one circuit breaker across all seven backends - a single struggling backend (e.g.
 * payment-service) would then trip the breaker for bookings, hotels, search, everything, which
 * defeats the entire point of a circuit breaker. A {@link Guard} is built once per backend segment
 * instead (see {@link #guardFor}), giving each backend fully independent state - see
 * docs/adr/0014-fault-tolerance.md.
 *
 * <p>Split into idempotent vs. non-idempotent on purpose: retrying a GET a struggling backend
 * already received is harmless, but retrying a POST/PUT/PATCH/DELETE risks duplicating a side
 * effect (e.g. creating two bookings) - only the idempotent guard configures {@code @Retry}.
 */
@ApplicationScoped
public class UpstreamProxyClient {

    private final WebClient webClient;
    private final Map<String, Guard> idempotentGuards = new ConcurrentHashMap<>();
    private final Map<String, Guard> nonIdempotentGuards = new ConcurrentHashMap<>();

    public UpstreamProxyClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public HttpResponse<Buffer> forwardIdempotent(
            String segment, HttpMethod method, String uri, MultiMap headers, Buffer body)
            throws Exception {
        return guard(idempotentGuards, segment, true)
                .call(() -> send(method, uri, headers, body), HttpResponse.class);
    }

    public HttpResponse<Buffer> forwardNonIdempotent(
            String segment, HttpMethod method, String uri, MultiMap headers, Buffer body)
            throws Exception {
        return guard(nonIdempotentGuards, segment, false)
                .call(() -> send(method, uri, headers, body), HttpResponse.class);
    }

    private Guard guard(Map<String, Guard> guardsBySegment, String segment, boolean idempotent) {
        return guardsBySegment.computeIfAbsent(segment, s -> buildGuard(idempotent));
    }

    private Guard buildGuard(boolean idempotent) {
        var builder =
                Guard.create()
                        .withTimeout()
                        .duration(idempotent ? 2 : 5, ChronoUnit.SECONDS)
                        .done()
                        .withCircuitBreaker()
                        .requestVolumeThreshold(4)
                        .failureRatio(0.5)
                        .delay(5, ChronoUnit.SECONDS)
                        .done()
                        .withBulkhead()
                        .limit(20)
                        .done();
        if (idempotent) {
            builder = builder.withRetry().maxRetries(2).delay(100, ChronoUnit.MILLIS).done();
        }
        return builder.build();
    }

    private HttpResponse<Buffer> send(
            HttpMethod method, String uri, MultiMap headers, Buffer body) {
        var request = webClient.requestAbs(method, uri);
        headers.forEach(entry -> request.putHeader(entry.getKey(), entry.getValue()));
        return request.sendBuffer(body).await().indefinitely();
    }
}

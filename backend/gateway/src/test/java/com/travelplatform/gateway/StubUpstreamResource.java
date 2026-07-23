package com.travelplatform.gateway;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stands in for a real backend behind the gateway - none of them run in this test suite, so
 * "booking" is remapped to this stub. Echoes back method/path/query/headers/body as JSON, so tests
 * can assert the gateway forwarded the request faithfully.
 *
 * <p>Two magic headers let a test script failure scenarios without a second server:
 *
 * <ul>
 *   <li>{@code X-Stub-Status} - the stub answers with this status code instead of 200.
 *   <li>{@code X-Stub-Test-Id} + {@code X-Stub-Slow-Until-Attempt} + {@code X-Stub-Delay-Ms} - the
 *       stub delays its response by {@code X-Stub-Delay-Ms} for every attempt up to and including
 *       {@code X-Stub-Slow-Until-Attempt} (attempts are counted per {@code X-Stub-Test-Id},
 *       starting at 1), then answers immediately from that attempt onward - used to force a real
 *       {@code TimeoutException} at the gateway and observe whether {@code @Retry} recovers from it
 *       (see docs/adr/0014-fault-tolerance.md).
 * </ul>
 */
public class StubUpstreamResource implements QuarkusTestResourceLifecycleManager {

    // Static: tests can't obtain a handle to the QuarkusTestResourceLifecycleManager instance
    // Quarkus constructs internally, so attempt counts are read via the static accessor below
    // instead.
    private static final Map<String, AtomicInteger> ATTEMPTS_BY_TEST_ID = new ConcurrentHashMap<>();

    private Vertx vertx;
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        vertx = Vertx.vertx();
        var future = new CompletableFuture<Integer>();

        server =
                vertx.createHttpServer()
                        .requestHandler(
                                request -> request.body(bodyResult -> handle(request, bodyResult)));
        server.listen(0, result -> future.complete(result.result().actualPort()));

        int port = future.join();
        return Map.of("gateway.upstream.booking", "http://localhost:" + port);
    }

    private void handle(
            io.vertx.core.http.HttpServerRequest request,
            io.vertx.core.AsyncResult<io.vertx.core.buffer.Buffer> bodyResult) {
        var delayMillis = delayFor(request);
        if (delayMillis > 0) {
            vertx.setTimer(delayMillis, id -> respond(request, bodyResult));
        } else {
            respond(request, bodyResult);
        }
    }

    private long delayFor(io.vertx.core.http.HttpServerRequest request) {
        var testId = request.getHeader("X-Stub-Test-Id");
        var slowUntilHeader = request.getHeader("X-Stub-Slow-Until-Attempt");
        var delayHeader = request.getHeader("X-Stub-Delay-Ms");
        if (testId == null || slowUntilHeader == null || delayHeader == null) {
            return 0;
        }
        var attempt =
                ATTEMPTS_BY_TEST_ID
                        .computeIfAbsent(testId, k -> new AtomicInteger(0))
                        .incrementAndGet();
        var slowUntil = Integer.parseInt(slowUntilHeader);
        return attempt <= slowUntil ? Long.parseLong(delayHeader) : 0;
    }

    private void respond(
            io.vertx.core.http.HttpServerRequest request,
            io.vertx.core.AsyncResult<io.vertx.core.buffer.Buffer> bodyResult) {
        var statusHeader = request.getHeader("X-Stub-Status");
        var status = statusHeader != null ? Integer.parseInt(statusHeader) : 200;
        var echo =
                new JsonObject()
                        .put("method", request.method().name())
                        .put("path", request.path())
                        .put("query", request.query())
                        .put("body", bodyResult.succeeded() ? bodyResult.result().toString() : "")
                        .put("authorization", request.getHeader("Authorization"));
        request.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(status)
                .end(echo.encode());
    }

    /**
     * Test-only escape hatch to read how many times a given {@code X-Stub-Test-Id} was attempted.
     */
    public static int attemptsFor(String testId) {
        return ATTEMPTS_BY_TEST_ID.getOrDefault(testId, new AtomicInteger(0)).get();
    }

    @Override
    public void stop() {
        if (server != null) {
            server.close();
        }
        if (vertx != null) {
            vertx.close();
        }
    }
}

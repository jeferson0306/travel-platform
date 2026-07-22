package com.travelplatform.gateway;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Stands in for a real backend behind the gateway - none of them run in this test suite, so
 * "booking" is remapped to this stub. Echoes back method/path/query/headers/body as JSON, so tests
 * can assert the gateway forwarded the request faithfully, and honors a magic {@code X-Stub-Status}
 * request header to let a test dictate the upstream response status for error-passthrough
 * assertions.
 */
public class StubUpstreamResource implements QuarkusTestResourceLifecycleManager {

    private Vertx vertx;
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        vertx = Vertx.vertx();
        var future = new CompletableFuture<Integer>();

        server =
                vertx.createHttpServer()
                        .requestHandler(
                                request ->
                                        request.body(
                                                bodyResult -> {
                                                    var statusHeader =
                                                            request.getHeader("X-Stub-Status");
                                                    var status =
                                                            statusHeader != null
                                                                    ? Integer.parseInt(statusHeader)
                                                                    : 200;
                                                    var echo =
                                                            new JsonObject()
                                                                    .put(
                                                                            "method",
                                                                            request.method().name())
                                                                    .put("path", request.path())
                                                                    .put("query", request.query())
                                                                    .put(
                                                                            "body",
                                                                            bodyResult.succeeded()
                                                                                    ? bodyResult
                                                                                            .result()
                                                                                            .toString()
                                                                                    : "")
                                                                    .put(
                                                                            "authorization",
                                                                            request.getHeader(
                                                                                    "Authorization"));
                                                    request.response()
                                                            .putHeader(
                                                                    "Content-Type",
                                                                    "application/json")
                                                            .setStatusCode(status)
                                                            .end(echo.encode());
                                                }));
        server.listen(0, result -> future.complete(result.result().actualPort()));

        int port = future.join();
        return Map.of("gateway.upstream.booking", "http://localhost:" + port);
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

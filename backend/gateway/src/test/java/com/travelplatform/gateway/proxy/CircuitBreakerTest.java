package com.travelplatform.gateway.proxy;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.travelplatform.gateway.ClosedPortUpstreamResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code hotels} is remapped to an unreachable port (see {@link ClosedPortUpstreamResource}), so
 * every call fails immediately with a connection error rather than a slow timeout - POST is used
 * deliberately ({@code forwardNonIdempotent} has no {@code @Retry}), so each outer HTTP request
 * produces exactly one circuit-breaker-tracked failure; a GET's {@code @Retry} would make each
 * request count as up to three. Only "hotels" is remapped here, not "bookings" (used by {@link
 * ProxyRoutesTest}/{@link FaultToleranceTest}) - each backend gets its own independent {@link
 * UpstreamProxyClient} {@code Guard}, so tripping this one has no effect on any other backend's
 * circuit, in this test suite or in production. See docs/adr/0014-fault-tolerance.md.
 */
@QuarkusTest
@QuarkusTestResource(ClosedPortUpstreamResource.class)
@DisplayName("CircuitBreaker")
class CircuitBreakerTest {

    @Test
    @DisplayName("opens after enough failures and fails fast instead of attempting a connection")
    void opensAfterRepeatedFailures() {
        // requestVolumeThreshold=4, failureRatio=0.5: four failing calls is enough to open it.
        for (int i = 0; i < 4; i++) {
            given().contentType("application/json")
                    .body("{}")
                    .when()
                    .post("/api/v1/hotels")
                    .then()
                    .statusCode(502);
        }

        given().contentType("application/json")
                .body("{}")
                .when()
                .post("/api/v1/hotels")
                .then()
                .statusCode(503)
                .body(equalTo("Upstream circuit breaker open"));

        // Confirms recovery: after the configured 5s delay the breaker half-opens and lets a
        // trial request through, which still fails (the port is still closed) with a normal 502.
        await().atMost(Duration.ofSeconds(8))
                .untilAsserted(
                        () ->
                                given().contentType("application/json")
                                        .body("{}")
                                        .when()
                                        .post("/api/v1/hotels")
                                        .then()
                                        .statusCode(502));
    }
}

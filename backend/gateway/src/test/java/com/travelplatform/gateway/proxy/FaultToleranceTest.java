package com.travelplatform.gateway.proxy;

import static io.restassured.RestAssured.given;

import com.travelplatform.gateway.StubUpstreamResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link UpstreamProxyClient}'s per-backend {@code Guard} timeout/retry policy end to
 * end: a genuinely hung upstream (not just an HTTP error status - see {@link
 * StubUpstreamResource}'s delay simulation) triggers a real {@code TimeoutException}, and whether a
 * retry is configured is what decides whether the gateway recovers - a GET does ({@code
 * forwardIdempotent}), a POST never does ({@code forwardNonIdempotent}, retrying it could duplicate
 * a side effect on the backend). See docs/adr/0014-fault-tolerance.md.
 */
@QuarkusTest
@QuarkusTestResource(StubUpstreamResource.class)
@DisplayName("Fault tolerance")
class FaultToleranceTest {

    @Test
    @DisplayName("a GET that times out once is retried and recovers")
    void idempotentRequestRetriesAfterATimeout() {
        var testId = UUID.randomUUID().toString();

        given().header("X-Stub-Test-Id", testId)
                .header("X-Stub-Slow-Until-Attempt", "1")
                .header("X-Stub-Delay-Ms", "2500") // exceeds forwardIdempotent's 2000ms @Timeout
                .when()
                .get("/api/v1/bookings/123")
                .then()
                .statusCode(200);

        Assertions.assertThat(StubUpstreamResource.attemptsFor(testId))
                .as("the first attempt should have timed out and a retry should have followed")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("a POST that times out is never retried, to avoid duplicating a side effect")
    void nonIdempotentRequestIsNeverRetried() {
        var testId = UUID.randomUUID().toString();

        given().header("X-Stub-Test-Id", testId)
                .header("X-Stub-Slow-Until-Attempt", "1")
                .header("X-Stub-Delay-Ms", "5500") // exceeds forwardNonIdempotent's 5000ms @Timeout
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/api/v1/bookings")
                .then()
                .statusCode(504);

        Assertions.assertThat(StubUpstreamResource.attemptsFor(testId))
                .as("a POST must only ever be attempted once, even after a timeout")
                .isEqualTo(1);
    }
}

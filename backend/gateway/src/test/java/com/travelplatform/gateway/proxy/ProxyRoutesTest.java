package com.travelplatform.gateway.proxy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.travelplatform.gateway.StubUpstreamResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the reverse-proxy path end to end: a real HTTP request into the gateway, forwarded to a
 * stub standing in for a backend (see {@link StubUpstreamResource}), which echoes back what it
 * received so the test can assert the gateway forwarded method/path/query/headers/body faithfully -
 * not just "some request got through."
 */
@QuarkusTest
@QuarkusTestResource(StubUpstreamResource.class)
@DisplayName("ProxyRoutes")
class ProxyRoutesTest {

    @Test
    @DisplayName("forwards method, path, query string and body to the resolved backend")
    void forwardsRequestFaithfully() {
        given().contentType("application/json")
                .body("{\"origin\":\"LIS\"}")
                .when()
                .post("/api/v1/bookings/search?limit=5")
                .then()
                .statusCode(200)
                .body("method", equalTo("POST"))
                .body("path", equalTo("/api/v1/bookings/search"))
                .body("query", equalTo("limit=5"))
                .body("body", equalTo("{\"origin\":\"LIS\"}"));
    }

    @Test
    @DisplayName("forwards the Authorization header through untouched when present")
    void forwardsAuthorizationHeader() {
        given().header("Authorization", "Bearer not-even-a-real-token-format")
                .when()
                .get("/api/v1/bookings/123")
                .then()
                .statusCode(401);
        // A malformed bearer token is rejected at the gateway itself (TokenValidator) before
        // ever reaching the stub - see TokenValidatorTest for the header-forwarding-when-valid
        // case, which needs a properly signed token.
    }

    @Test
    @DisplayName("a request with no Authorization header passes through untouched")
    void noAuthorizationHeaderPassesThrough() {
        given().when()
                .get("/api/v1/bookings/123")
                .then()
                .statusCode(200)
                .body("authorization", nullValue());
    }

    @Test
    @DisplayName("returns the upstream's status code verbatim")
    void returnsUpstreamStatusVerbatim() {
        given().header("X-Stub-Status", "503")
                .when()
                .get("/api/v1/bookings/123")
                .then()
                .statusCode(503);
    }

    @Test
    @DisplayName("an unmapped path prefix returns 404 without reaching any backend")
    void unmappedPrefixReturns404() {
        given().when().get("/api/v1/not-a-real-service/anything").then().statusCode(404);
    }

    @Test
    @DisplayName("proxies an upstream response with a genuinely empty body without throwing")
    void proxiesEmptyUpstreamBody() {
        // Regression test: upstreamResponse.bodyAsBuffer() returns null for a body-less upstream
        // response, which threw a NullPointerException in ProxyRoutes.forward before it
        // null-checked
        // - observed directly under real traffic, caught here instead of only in production.
        given().header("X-Stub-Empty-Body", "true")
                .when()
                .get("/api/v1/bookings/123")
                .then()
                .statusCode(200)
                .body(equalTo(""));
    }

    @Test
    @DisplayName("answers a CORS preflight request without reaching any backend")
    void answersCorsPreflight() {
        given().header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .when()
                .options("/api/v1/bookings")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost:5173");
    }

    @Test
    @DisplayName("an actual (non-preflight) POST request with a matching Origin header succeeds")
    void actualRequestWithOriginHeaderSucceeds() {
        // Regression test: Quarkus's own declarative CORS filter rejected this exact scenario with
        // a 403 on the pinned version (see application.yml), which the preflight-only test above
        // never caught since OPTIONS was answered separately. CORS is now handled directly in
        // ProxyRoutes, so an actual request with a matching Origin must succeed and carry the
        // Access-Control-Allow-Origin header on the real response.
        given().contentType("application/json")
                .header("Origin", "http://localhost:5173")
                .body("{\"origin\":\"LIS\"}")
                .when()
                .post("/api/v1/bookings/search")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost:5173");
    }
}

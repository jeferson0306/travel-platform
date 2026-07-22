package com.travelplatform.identity.infrastructure.observability;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Verifies the health and metrics endpoints ROADMAP M6 requires are actually exposed. */
@QuarkusTest
@DisplayName("Observability endpoints")
class ObservabilityEndpointsTest {

    @Test
    @DisplayName("/health reports UP")
    void healthEndpointReportsUp() {
        given().when().get("/health").then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("/health/ready reports UP")
    void readinessEndpointReportsUp() {
        given().when().get("/health/ready").then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("/health/live reports UP")
    void livenessEndpointReportsUp() {
        given().when().get("/health/live").then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("/q/metrics exposes Prometheus-format output")
    void metricsEndpointExposesPrometheusFormat() {
        given().when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .body(org.hamcrest.Matchers.containsString("# HELP"));
    }
}

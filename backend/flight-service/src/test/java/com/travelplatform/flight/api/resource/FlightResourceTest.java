package com.travelplatform.flight.api.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.travelplatform.flight.api.dto.CreateFlightRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises search (public) and create (role-protected) end to end over HTTP against a real MongoDB
 * (Quarkus Dev Services). Tokens are minted with the test-only private key in
 * src/test/resources/privateKey.pem, which mirrors identity-service's real signing key
 * (smallrye-jwt-build, test scope only - production code never builds a token here) so this service
 * can be tested in isolation without running identity-service, while still exercising the real
 * RS256 verification config in application.yml (publicKey.pem).
 */
@QuarkusTest
@DisplayName("FlightResource")
class FlightResourceTest {

    private String tokenWithRole(String role) {
        return Jwt.issuer("travel-platform-identity")
                .subject(UUID.randomUUID().toString())
                .groups(role)
                .sign("privateKey.pem");
    }

    @Test
    void searchIsPublicAndReturnsCreatedFlights() {
        var origin = "LIS";
        var destination = "OPO";
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        var arrival = departure.plus(1, ChronoUnit.HOURS);

        given().header("Authorization", "Bearer " + tokenWithRole("MANAGER"))
                .contentType("application/json")
                .body(
                        new CreateFlightRequest(
                                origin,
                                destination,
                                departure,
                                arrival,
                                new BigDecimal("120.00"),
                                "EUR",
                                50))
                .post("/api/v1/flights")
                .then()
                .statusCode(201);

        given().queryParam("origin", origin)
                .queryParam("destination", destination)
                .when()
                .get("/api/v1/flights")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].origin", equalTo(origin));
    }

    @Test
    void searchFiltersByDepartureDateWhenGiven() {
        var origin = "LIS";
        var destination = "MAD";
        var matchingDeparture = Instant.parse("2027-03-10T09:00:00Z");
        var otherDayDeparture = Instant.parse("2027-03-11T09:00:00Z");

        given().header("Authorization", "Bearer " + tokenWithRole("MANAGER"))
                .contentType("application/json")
                .body(
                        new CreateFlightRequest(
                                origin,
                                destination,
                                matchingDeparture,
                                matchingDeparture.plus(1, ChronoUnit.HOURS),
                                new BigDecimal("100.00"),
                                "EUR",
                                10))
                .post("/api/v1/flights")
                .then()
                .statusCode(201);
        given().header("Authorization", "Bearer " + tokenWithRole("MANAGER"))
                .contentType("application/json")
                .body(
                        new CreateFlightRequest(
                                origin,
                                destination,
                                otherDayDeparture,
                                otherDayDeparture.plus(1, ChronoUnit.HOURS),
                                new BigDecimal("100.00"),
                                "EUR",
                                10))
                .post("/api/v1/flights")
                .then()
                .statusCode(201);

        given().queryParam("origin", origin)
                .queryParam("destination", destination)
                .queryParam("departureDate", "2027-03-10")
                .when()
                .get("/api/v1/flights")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].departureAt", equalTo("2027-03-10T09:00:00Z"));
    }

    @Test
    void createWithoutTokenIsRejected() {
        // No Authorization header at all is rejected by Quarkus's HTTP auth challenge before
        // the request ever reaches JAX-RS, so UnauthenticatedExceptionMapper's canonical body
        // does not apply here - only to a present-but-invalid/insufficient-role token (see the
        // other tests). Status code is still the contract that matters for this case.
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        given().contentType("application/json")
                .body(
                        new CreateFlightRequest(
                                "LIS",
                                "GRU",
                                departure,
                                departure.plus(1, ChronoUnit.HOURS),
                                new BigDecimal("1"),
                                "EUR",
                                1))
                .when()
                .post("/api/v1/flights")
                .then()
                .statusCode(401);
    }

    @Test
    void createWithInsufficientRoleIsForbidden() {
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        given().header("Authorization", "Bearer " + tokenWithRole("USER"))
                .contentType("application/json")
                .body(
                        new CreateFlightRequest(
                                "LIS",
                                "GRU",
                                departure,
                                departure.plus(1, ChronoUnit.HOURS),
                                new BigDecimal("1"),
                                "EUR",
                                1))
                .when()
                .post("/api/v1/flights")
                .then()
                .statusCode(403)
                .body("error", equalTo("FORBIDDEN"));
    }

    @Test
    void rejectsInvalidPayload() {
        given().header("Authorization", "Bearer " + tokenWithRole("ADMIN"))
                .contentType("application/json")
                .body(
                        new CreateFlightRequest(
                                "LISBON",
                                "GRU",
                                Instant.now(),
                                Instant.now(),
                                new BigDecimal("-1"),
                                "EUR",
                                -5))
                .when()
                .post("/api/v1/flights")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }
}

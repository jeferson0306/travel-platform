package com.travelplatform.hotel.api.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.travelplatform.hotel.api.dto.CreateHotelRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises search (public) and create (role-protected) end to end over HTTP against a real MongoDB
 * (Quarkus Dev Services). Tokens are minted with the test-only private key in
 * src/test/resources/privateKey.pem, mirroring identity-service's real signing key - see
 * flight-service's equivalent test for the full rationale.
 */
@QuarkusTest
@DisplayName("HotelResource")
class HotelResourceTest {

    private String tokenWithRole(String role) {
        return Jwt.issuer("travel-platform-identity")
                .subject(UUID.randomUUID().toString())
                .groups(role)
                .sign("privateKey.pem");
    }

    @Test
    void searchIsPublicAndReturnsCreatedHotels() {
        var city = "Porto";

        given().header("Authorization", "Bearer " + tokenWithRole("MANAGER"))
                .contentType("application/json")
                .body(
                        new CreateHotelRequest(
                                "Porto Riverside",
                                city,
                                new BigDecimal("95.00"),
                                "EUR",
                                15,
                                "Rua do Ouro 50",
                                4,
                                List.of("Free WiFi", "Breakfast included"),
                                "A riverside hotel in Porto.",
                                4.5,
                                200))
                .post("/api/v1/hotels")
                .then()
                .statusCode(201);

        given().queryParam("city", city)
                .when()
                .get("/api/v1/hotels")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].city", equalTo(city));
    }

    @Test
    void createWithoutTokenIsRejected() {
        given().contentType("application/json")
                .body(
                        new CreateHotelRequest(
                                "Hotel X",
                                "Lisbon",
                                new BigDecimal("1"),
                                "EUR",
                                1,
                                null,
                                null,
                                null,
                                null,
                                null,
                                0))
                .when()
                .post("/api/v1/hotels")
                .then()
                .statusCode(401);
    }

    @Test
    void createWithInsufficientRoleIsForbidden() {
        given().header("Authorization", "Bearer " + tokenWithRole("USER"))
                .contentType("application/json")
                .body(
                        new CreateHotelRequest(
                                "Hotel X",
                                "Lisbon",
                                new BigDecimal("1"),
                                "EUR",
                                1,
                                null,
                                null,
                                null,
                                null,
                                null,
                                0))
                .when()
                .post("/api/v1/hotels")
                .then()
                .statusCode(403)
                .body("error", equalTo("FORBIDDEN"));
    }

    @Test
    void rejectsInvalidPayload() {
        given().header("Authorization", "Bearer " + tokenWithRole("ADMIN"))
                .contentType("application/json")
                .body(
                        new CreateHotelRequest(
                                "",
                                "Lisbon",
                                new BigDecimal("-1"),
                                "EU",
                                -5,
                                null,
                                null,
                                null,
                                null,
                                null,
                                0))
                .when()
                .post("/api/v1/hotels")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void createWithoutStarRatingDefaultsToThree() {
        given().header("Authorization", "Bearer " + tokenWithRole("MANAGER"))
                .contentType("application/json")
                .body(
                        new CreateHotelRequest(
                                "Coimbra Inn",
                                "Coimbra",
                                new BigDecimal("60.00"),
                                "EUR",
                                10,
                                null,
                                null,
                                null,
                                null,
                                null,
                                0))
                .when()
                .post("/api/v1/hotels")
                .then()
                .statusCode(201);

        given().queryParam("city", "Coimbra")
                .when()
                .get("/api/v1/hotels")
                .then()
                .statusCode(200)
                .body("[0].starRating", equalTo(3))
                .body("[0].amenities", org.hamcrest.Matchers.empty());
    }
}

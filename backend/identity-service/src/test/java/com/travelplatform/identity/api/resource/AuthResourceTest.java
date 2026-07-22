package com.travelplatform.identity.api.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.travelplatform.identity.api.dto.LoginRequest;
import com.travelplatform.identity.api.dto.RegisterUserRequest;
import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Exercises registration and login end to end over HTTP, against a real MongoDB started by Quarkus
 * Dev Services (Testcontainers under the hood) - no mocks.
 */
@QuarkusTest
class AuthResourceTest {

    @Test
    void registersAndLogsInSuccessfully() {
        var email = "traveler-" + UUID.randomUUID() + "@example.com";

        given().contentType("application/json")
                .body(new RegisterUserRequest(email, "s3cret-pass"))
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .body("userId", notNullValue());

        given().contentType("application/json")
                .body(new LoginRequest(email, "s3cret-pass"))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("expiresInSeconds", equalTo(3600));
    }

    @Test
    void rejectsDuplicateRegistration() {
        var email = "traveler-" + UUID.randomUUID() + "@example.com";
        var request = new RegisterUserRequest(email, "s3cret-pass");

        given().contentType("application/json").body(request).post("/api/v1/auth/register");

        given().contentType("application/json")
                .body(request)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(409)
                .body("error", equalTo("CONFLICT"));
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        var email = "traveler-" + UUID.randomUUID() + "@example.com";
        given().contentType("application/json")
                .body(new RegisterUserRequest(email, "s3cret-pass"))
                .post("/api/v1/auth/register");

        given().contentType("application/json")
                .body(new LoginRequest(email, "wrong-password"))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("error", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    void rejectsRegistrationWithInvalidPayload() {
        given().contentType("application/json")
                .body(new RegisterUserRequest("not-an-email", "short"))
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"))
                .body("details", notNullValue());
    }

    @Test
    void echoesBackACorrelationIdForTracing() {
        var email = "traveler-" + UUID.randomUUID() + "@example.com";

        given().contentType("application/json")
                .header("X-Correlation-Id", "test-correlation-id")
                .body(new RegisterUserRequest(email, "s3cret-pass"))
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .header("X-Correlation-Id", equalTo("test-correlation-id"));
    }

    @Test
    void generatesACorrelationIdWhenCallerDoesNotSupplyOne() {
        var email = "traveler-" + UUID.randomUUID() + "@example.com";

        given().contentType("application/json")
                .body(new RegisterUserRequest(email, "s3cret-pass"))
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .header("X-Correlation-Id", notNullValue());
    }
}

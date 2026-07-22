package com.travelplatform.payment.api.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.mongodb.client.MongoClient;
import com.travelplatform.payment.domain.payment.BookingId;
import com.travelplatform.payment.domain.payment.Money;
import com.travelplatform.payment.domain.payment.Payment;
import com.travelplatform.payment.infrastructure.persistence.mongo.PaymentDocumentMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Exercises the read-only payment lookup end to end over HTTP against a real MongoDB (Quarkus Dev
 * Services). Tokens are minted with the test-only private key in src/test/resources/privateKey.pem
 * - see flight-service's equivalent test for the full rationale.
 */
@QuarkusTest
class PaymentResourceTest {

    @Inject MongoClient mongoClient;

    private String tokenWithRole(String role) {
        return Jwt.issuer("travel-platform-identity")
                .subject(UUID.randomUUID().toString())
                .groups(role)
                .sign("privateKey.pem");
    }

    private String seedAnAuthorizedPayment() {
        var bookingId = new BookingId(UUID.randomUUID());
        var payment = Payment.authorize(bookingId, new Money(new BigDecimal("450.00"), "EUR"));
        mongoClient
                .getDatabase("payment")
                .getCollection("payments")
                .insertOne(PaymentDocumentMapper.toDocument(payment));
        return bookingId.value().toString();
    }

    @Test
    void returnsThePaymentForASupportRole() {
        var bookingId = seedAnAuthorizedPayment();

        given().header("Authorization", "Bearer " + tokenWithRole("SUPPORT"))
                .when()
                .get("/api/v1/payments/" + bookingId)
                .then()
                .statusCode(200)
                .body("bookingId", equalTo(bookingId))
                .body("status", equalTo("AUTHORIZED"));
    }

    @Test
    void withoutTokenIsRejected() {
        given().when().get("/api/v1/payments/" + UUID.randomUUID()).then().statusCode(401);
    }

    @Test
    void insufficientRoleIsForbidden() {
        given().header("Authorization", "Bearer " + tokenWithRole("USER"))
                .when()
                .get("/api/v1/payments/" + UUID.randomUUID())
                .then()
                .statusCode(403)
                .body("error", equalTo("FORBIDDEN"));
    }

    @Test
    void unknownBookingReturnsNotFound() {
        given().header("Authorization", "Bearer " + tokenWithRole("ADMIN"))
                .when()
                .get("/api/v1/payments/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("error", equalTo("NOT_FOUND"));
    }
}

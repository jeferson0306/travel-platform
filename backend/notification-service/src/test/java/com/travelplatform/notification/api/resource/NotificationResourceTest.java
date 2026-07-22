package com.travelplatform.notification.api.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.mongodb.client.MongoClient;
import com.travelplatform.notification.domain.notification.BookingId;
import com.travelplatform.notification.domain.notification.Email;
import com.travelplatform.notification.domain.notification.Notification;
import com.travelplatform.notification.domain.notification.NotificationType;
import com.travelplatform.notification.infrastructure.persistence.mongo.NotificationDocumentMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the read-only notification lookup end to end over HTTP against a real MongoDB (Quarkus
 * Dev Services). Tokens are minted with the test-only private key in
 * src/test/resources/privateKey.pem - see flight-service's equivalent test for the full rationale.
 */
@QuarkusTest
@DisplayName("NotificationResource")
class NotificationResourceTest {

    @Inject MongoClient mongoClient;

    private String tokenWithRole(String role) {
        return Jwt.issuer("travel-platform-identity")
                .subject(UUID.randomUUID().toString())
                .groups(role)
                .sign("privateKey.pem");
    }

    private String seedASentNotification() {
        var bookingId = new BookingId(UUID.randomUUID());
        var notification =
                Notification.sent(
                        bookingId,
                        new Email("traveler@example.com"),
                        NotificationType.BOOKING_CONFIRMATION);
        mongoClient
                .getDatabase("notification")
                .getCollection("notifications")
                .insertOne(NotificationDocumentMapper.toDocument(notification));
        return bookingId.value().toString();
    }

    @Test
    void returnsTheNotificationForASupportRole() {
        var bookingId = seedASentNotification();

        given().header("Authorization", "Bearer " + tokenWithRole("SUPPORT"))
                .when()
                .get("/api/v1/notifications/" + bookingId)
                .then()
                .statusCode(200)
                .body("bookingId", equalTo(bookingId))
                .body("recipient", equalTo("traveler@example.com"))
                .body("status", equalTo("SENT"));
    }

    @Test
    void withoutTokenIsRejected() {
        given().when().get("/api/v1/notifications/" + UUID.randomUUID()).then().statusCode(401);
    }

    @Test
    void insufficientRoleIsForbidden() {
        given().header("Authorization", "Bearer " + tokenWithRole("USER"))
                .when()
                .get("/api/v1/notifications/" + UUID.randomUUID())
                .then()
                .statusCode(403)
                .body("error", equalTo("FORBIDDEN"));
    }

    @Test
    void unknownBookingReturnsNotFound() {
        given().header("Authorization", "Bearer " + tokenWithRole("ADMIN"))
                .when()
                .get("/api/v1/notifications/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("error", equalTo("NOT_FOUND"));
    }
}

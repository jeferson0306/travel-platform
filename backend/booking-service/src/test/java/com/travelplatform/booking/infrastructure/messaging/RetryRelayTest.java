package com.travelplatform.booking.infrastructure.messaging;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.travelplatform.booking.api.dto.CreateBookingRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link RetryRelay#relay()} directly (package-visible, called synchronously here instead of
 * waiting on its real 10s @Scheduled trigger) - see flight-service's equivalent test for the full
 * rationale. Uses an unknown bookingId to force a genuine, repeatable failure
 * (BookingNotFoundException) for the backoff/dead-letter cases, since confirm()/cancel() have no
 * other realistic failure mode once the retry task exists.
 */
@QuarkusTest
@DisplayName("RetryRelay")
class RetryRelayTest {

    @Inject RetryRelay retryRelay;
    @Inject MongoClient mongoClient;
    @Inject @Any InMemoryConnector connector;

    private MongoCollection<Document> retryTasks;
    private MongoCollection<Document> deadLetters;

    @BeforeEach
    void setUp() {
        var db = mongoClient.getDatabase("booking");
        retryTasks = db.getCollection("retry_tasks");
        deadLetters = db.getCollection("dead_letters");
        connector.sink("payment-authorized-dlq").clear();
    }

    private String createBooking() {
        var token =
                Jwt.issuer("travel-platform-identity")
                        .subject(UUID.randomUUID().toString())
                        .sign("privateKey.pem");
        return given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(
                        new CreateBookingRequest(
                                "traveler@example.com",
                                "FLIGHT",
                                UUID.randomUUID().toString(),
                                1,
                                new BigDecimal("450.00"),
                                "EUR",
                                null))
                .post("/api/v1/bookings")
                .then()
                .extract()
                .path("bookingId");
    }

    private void insertRetryTask(String action, String bookingId, int attempts) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("action", action)
                        .append("bookingId", bookingId)
                        .append("attempts", attempts)
                        .append("lastError", "seeded by RetryRelayTest")
                        .append("nextAttemptAt", Date.from(Instant.now().minusSeconds(1))));
    }

    @Test
    @DisplayName("a retry that now succeeds confirms the booking and clears the task")
    void successfulPaymentAuthorizedRetryClearsTheTask() {
        var bookingId = createBooking();
        insertRetryTask("payment-authorized", bookingId, 0);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var booking =
                mongoClient
                        .getDatabase("booking")
                        .getCollection("bookings")
                        .find(Filters.eq("_id", bookingId))
                        .first();
        assertThat(booking.getString("status")).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("a retry that still fails increments attempts and backs off exponentially")
    void failedRetryBacksOffExponentially() {
        var unknownBookingId = UUID.randomUUID().toString();
        insertRetryTask("payment-authorized", unknownBookingId, 0);
        var before = Instant.now();

        retryRelay.relay();

        var task = retryTasks.find(Filters.eq("bookingId", unknownBookingId)).first();
        assertThat(task).isNotNull();
        assertThat(task.getInteger("attempts")).isEqualTo(1);
        assertThat(task.getDate("nextAttemptAt").toInstant())
                .isAfter(before.plusSeconds(19))
                .isBefore(before.plusSeconds(25));
    }

    @Test
    @DisplayName(
            "exhausting all attempts dead-letters the task and publishes to the matching DLQ topic")
    void exhaustingRetriesMovesToDeadLettersAndPublishesToDlq() {
        var unknownBookingId = UUID.randomUUID().toString();
        insertRetryTask("payment-authorized", unknownBookingId, 4);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", unknownBookingId)).first()).isNull();
        var deadLetter = deadLetters.find(Filters.eq("bookingId", unknownBookingId)).first();
        assertThat(deadLetter).isNotNull();
        assertThat(deadLetter.getInteger("attempts")).isEqualTo(5);

        InMemorySink<String> dlq = connector.sink("payment-authorized-dlq");
        assertThat(dlq.received()).hasSize(1);
        assertThat(dlq.received().get(0).getPayload()).contains(unknownBookingId);
    }

    @Test
    @DisplayName("a payment-failed retry that now succeeds cancels the booking")
    void successfulPaymentFailedRetryCancelsTheBooking() {
        var bookingId = createBooking();
        insertRetryTask("payment-failed", bookingId, 0);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var booking =
                mongoClient
                        .getDatabase("booking")
                        .getCollection("bookings")
                        .find(Filters.eq("_id", bookingId))
                        .first();
        assertThat(booking.getString("status")).isEqualTo("CANCELLED");
    }
}

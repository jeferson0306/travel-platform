package com.travelplatform.flight.infrastructure.messaging;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.travelplatform.flight.api.dto.CreateFlightRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link RetryRelay#relay()} directly (package-visible, called synchronously here instead of
 * waiting on its real 10s @Scheduled trigger) to deterministically exercise the three outcomes a
 * retried task can have: succeeds and clears, fails and backs off, or exhausts its attempts and is
 * dead-lettered - the path flagged as the least-covered in the M10/M11 test audit.
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
        var db = mongoClient.getDatabase("flight");
        retryTasks = db.getCollection("retry_tasks");
        deadLetters = db.getCollection("dead_letters");
        connector.sink("booking-created-dlq").clear();
    }

    private String tokenWithRole(String role) {
        return Jwt.issuer("travel-platform-identity")
                .subject(UUID.randomUUID().toString())
                .groups(role)
                .sign("privateKey.pem");
    }

    private String createFlight(int availableSeats) {
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        return given().header("Authorization", "Bearer " + tokenWithRole("MANAGER"))
                .contentType("application/json")
                .body(
                        new CreateFlightRequest(
                                "LIS",
                                "GRU",
                                departure,
                                departure.plus(10, ChronoUnit.HOURS),
                                new BigDecimal("450.00"),
                                "EUR",
                                availableSeats))
                .post("/api/v1/flights")
                .then()
                .statusCode(201)
                .extract()
                .path("flightId");
    }

    private void insertRetryTask(String bookingId, String flightId, int quantity, int attempts) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("bookingId", bookingId)
                        .append("itemId", flightId)
                        .append("quantity", quantity)
                        .append("attempts", attempts)
                        .append("lastError", "seeded by RetryRelayTest")
                        // Due now, so relay() picks it up immediately instead of waiting on
                        // backoff.
                        .append("nextAttemptAt", Date.from(Instant.now().minusSeconds(1))));
    }

    @Test
    @DisplayName("a retry that now succeeds clears the task and decrements inventory")
    void successfulRetryClearsTheTask() {
        var flightId = createFlight(5);
        var bookingId = UUID.randomUUID().toString();
        insertRetryTask(bookingId, flightId, 3, 0);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var flight =
                mongoClient
                        .getDatabase("flight")
                        .getCollection("flights")
                        .find(Filters.eq("_id", flightId))
                        .first();
        assertThat(flight.getInteger("availableSeats")).isEqualTo(2);
    }

    @Test
    @DisplayName("a retry that still fails increments attempts and backs off exponentially")
    void failedRetryBacksOffExponentially() {
        var flightId = createFlight(1);
        var bookingId = UUID.randomUUID().toString();
        insertRetryTask(bookingId, flightId, 5, 0);
        var before = Instant.now();

        retryRelay.relay();

        var task = retryTasks.find(Filters.eq("bookingId", bookingId)).first();
        assertThat(task).isNotNull();
        assertThat(task.getInteger("attempts")).isEqualTo(1);
        // BASE_BACKOFF_SECONDS(10) * 2^1 = 20s.
        assertThat(task.getDate("nextAttemptAt").toInstant())
                .isAfter(before.plusSeconds(19))
                .isBefore(before.plusSeconds(25));
    }

    @Test
    @DisplayName("exhausting all attempts dead-letters the task and publishes to the DLQ topic")
    void exhaustingRetriesMovesToDeadLettersAndPublishesToDlq() {
        var flightId = createFlight(1);
        var bookingId = UUID.randomUUID().toString();
        // attempts=4 going in: this attempt becomes the 5th (MAX_ATTEMPTS), the last one allowed.
        insertRetryTask(bookingId, flightId, 5, 4);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var deadLetter = deadLetters.find(Filters.eq("bookingId", bookingId)).first();
        assertThat(deadLetter).isNotNull();
        assertThat(deadLetter.getInteger("attempts")).isEqualTo(5);

        InMemorySink<String> dlq = connector.sink("booking-created-dlq");
        assertThat(dlq.received()).hasSize(1);
        assertThat(dlq.received().get(0).getPayload()).contains(bookingId);
    }
}

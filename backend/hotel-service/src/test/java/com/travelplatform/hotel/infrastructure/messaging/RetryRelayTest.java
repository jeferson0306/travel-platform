package com.travelplatform.hotel.infrastructure.messaging;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.travelplatform.hotel.api.dto.CreateHotelRequest;
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
 * waiting on its real 10s @Scheduled trigger) to deterministically exercise the three outcomes a
 * retried task can have: succeeds and clears, fails and backs off, or exhausts its attempts and is
 * dead-lettered - see flight-service's equivalent test for the full rationale.
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
        var db = mongoClient.getDatabase("hotel");
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

    private String createHotel(int availableRooms) {
        return given().header("Authorization", "Bearer " + tokenWithRole("MANAGER"))
                .contentType("application/json")
                .body(
                        new CreateHotelRequest(
                                "Porto Riverside",
                                "Porto",
                                new BigDecimal("95.00"),
                                "EUR",
                                availableRooms))
                .post("/api/v1/hotels")
                .then()
                .statusCode(201)
                .extract()
                .path("hotelId");
    }

    private void insertRetryTask(String bookingId, String hotelId, int quantity, int attempts) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("bookingId", bookingId)
                        .append("itemId", hotelId)
                        .append("quantity", quantity)
                        .append("attempts", attempts)
                        .append("lastError", "seeded by RetryRelayTest")
                        .append("nextAttemptAt", Date.from(Instant.now().minusSeconds(1))));
    }

    @Test
    @DisplayName("a retry that now succeeds clears the task and decrements inventory")
    void successfulRetryClearsTheTask() {
        var hotelId = createHotel(5);
        var bookingId = UUID.randomUUID().toString();
        insertRetryTask(bookingId, hotelId, 3, 0);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var hotel =
                mongoClient
                        .getDatabase("hotel")
                        .getCollection("hotels")
                        .find(Filters.eq("_id", hotelId))
                        .first();
        assertThat(hotel.getInteger("availableRooms")).isEqualTo(2);
    }

    @Test
    @DisplayName("a retry that still fails increments attempts and backs off exponentially")
    void failedRetryBacksOffExponentially() {
        var hotelId = createHotel(1);
        var bookingId = UUID.randomUUID().toString();
        insertRetryTask(bookingId, hotelId, 5, 0);
        var before = Instant.now();

        retryRelay.relay();

        var task = retryTasks.find(Filters.eq("bookingId", bookingId)).first();
        assertThat(task).isNotNull();
        assertThat(task.getInteger("attempts")).isEqualTo(1);
        assertThat(task.getDate("nextAttemptAt").toInstant())
                .isAfter(before.plusSeconds(19))
                .isBefore(before.plusSeconds(25));
    }

    @Test
    @DisplayName("exhausting all attempts dead-letters the task and publishes to the DLQ topic")
    void exhaustingRetriesMovesToDeadLettersAndPublishesToDlq() {
        var hotelId = createHotel(1);
        var bookingId = UUID.randomUUID().toString();
        insertRetryTask(bookingId, hotelId, 5, 4);

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

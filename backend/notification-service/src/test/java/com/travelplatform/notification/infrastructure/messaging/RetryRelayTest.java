package com.travelplatform.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
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
 * rationale. Only one kind of retry task exists here, so unlike payment/booking-service there is no
 * action-field dispatch to cover.
 */
@QuarkusTest
@DisplayName("RetryRelay")
class RetryRelayTest {

    @Inject RetryRelay retryRelay;
    @Inject MongoClient mongoClient;
    @Inject @Any InMemoryConnector connector;

    private MongoCollection<Document> retryTasks;
    private MongoCollection<Document> deadLetters;
    private MongoCollection<Document> notifications;

    @BeforeEach
    void setUp() {
        var db = mongoClient.getDatabase("notification");
        retryTasks = db.getCollection("retry_tasks");
        deadLetters = db.getCollection("dead_letters");
        notifications = db.getCollection("notifications");
        connector.sink("booking-confirmed-dlq").clear();
    }

    private void insertRetryTask(String bookingId, String recipientEmail, int attempts) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("bookingId", bookingId)
                        .append("recipientEmail", recipientEmail)
                        .append("attempts", attempts)
                        .append("lastError", "seeded by RetryRelayTest")
                        .append("nextAttemptAt", Date.from(Instant.now().minusSeconds(1))));
    }

    @Test
    @DisplayName(
            "a retry that now succeeds sends the email, stores the notification and clears the task")
    void successfulRetryClearsTheTask() {
        var bookingId = UUID.randomUUID().toString();
        insertRetryTask(bookingId, "traveler@example.com", 0);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var notification = notifications.find(Filters.eq("bookingId", bookingId)).first();
        assertThat(notification).isNotNull();
        assertThat(notification.getString("status")).isEqualTo("SENT");
    }

    @Test
    @DisplayName("a retry that still fails increments attempts and backs off exponentially")
    void failedRetryBacksOffExponentially() {
        var bookingId = UUID.randomUUID().toString();
        // An invalid email makes SendBookingConfirmationService throw before the gateway is even
        // reached (Email's constructor validates the format), simulating a persistent failure.
        insertRetryTask(bookingId, "not-an-email", 0);
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
    @DisplayName(
            "exhausting all attempts dead-letters the task and publishes to the matching DLQ topic")
    void exhaustingRetriesMovesToDeadLettersAndPublishesToDlq() {
        var bookingId = UUID.randomUUID().toString();
        insertRetryTask(bookingId, "not-an-email", 4);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var deadLetter = deadLetters.find(Filters.eq("bookingId", bookingId)).first();
        assertThat(deadLetter).isNotNull();
        assertThat(deadLetter.getInteger("attempts")).isEqualTo(5);

        InMemorySink<String> dlq = connector.sink("booking-confirmed-dlq");
        assertThat(dlq.received()).hasSize(1);
        assertThat(dlq.received().get(0).getPayload()).contains(bookingId);
    }
}

package com.travelplatform.payment.infrastructure.messaging;

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
 * rationale. payment-service's RetryRelay is the most complex of the three: it dispatches on an
 * {@code action} field between authorize (needs a stored amount) and refund (doesn't).
 */
@QuarkusTest
@DisplayName("RetryRelay")
class RetryRelayTest {

    @Inject RetryRelay retryRelay;
    @Inject MongoClient mongoClient;
    @Inject @Any InMemoryConnector connector;

    private MongoCollection<Document> retryTasks;
    private MongoCollection<Document> deadLetters;
    private MongoCollection<Document> payments;

    @BeforeEach
    void setUp() {
        var db = mongoClient.getDatabase("payment");
        retryTasks = db.getCollection("retry_tasks");
        deadLetters = db.getCollection("dead_letters");
        payments = db.getCollection("payments");
        connector.sink("booking-cancelled-dlq").clear();
    }

    private void insertAuthorizeRetryTask(String bookingId, int attempts) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("action", "booking-created")
                        .append("bookingId", bookingId)
                        .append("amountValue", "450.00")
                        .append("amountCurrency", "EUR")
                        .append("attempts", attempts)
                        .append("lastError", "seeded by RetryRelayTest")
                        .append("nextAttemptAt", Date.from(Instant.now().minusSeconds(1))));
    }

    private void insertRefundRetryTask(String bookingId, int attempts) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("action", "booking-cancelled")
                        .append("bookingId", bookingId)
                        .append("attempts", attempts)
                        .append("lastError", "seeded by RetryRelayTest")
                        .append("nextAttemptAt", Date.from(Instant.now().minusSeconds(1))));
    }

    @Test
    @DisplayName(
            "a booking-created retry that now succeeds authorizes the payment and clears the task")
    void successfulAuthorizeRetryClearsTheTask() {
        var bookingId = UUID.randomUUID().toString();
        insertAuthorizeRetryTask(bookingId, 0);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var payment = payments.find(Filters.eq("bookingId", bookingId)).first();
        assertThat(payment).isNotNull();
        assertThat(payment.getString("status")).isEqualTo("AUTHORIZED");
    }

    @Test
    @DisplayName("a booking-cancelled retry that now succeeds refunds an authorized payment")
    void successfulRefundRetryClearsTheTask() {
        var bookingId = UUID.randomUUID().toString();
        insertAuthorizeRetryTask(bookingId, 0);
        retryRelay.relay();
        insertRefundRetryTask(bookingId, 0);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", bookingId)).first()).isNull();
        var payment = payments.find(Filters.eq("bookingId", bookingId)).first();
        assertThat(payment.getString("status")).isEqualTo("REFUNDED");
    }

    @Test
    @DisplayName("a retry that still fails increments attempts and backs off exponentially")
    void failedRetryBacksOffExponentially() {
        var unknownBookingId = UUID.randomUUID().toString();
        insertRefundRetryTask(unknownBookingId, 0);
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
        insertRefundRetryTask(unknownBookingId, 4);

        retryRelay.relay();

        assertThat(retryTasks.find(Filters.eq("bookingId", unknownBookingId)).first()).isNull();
        var deadLetter = deadLetters.find(Filters.eq("bookingId", unknownBookingId)).first();
        assertThat(deadLetter).isNotNull();
        assertThat(deadLetter.getInteger("attempts")).isEqualTo(5);

        InMemorySink<String> dlq = connector.sink("booking-cancelled-dlq");
        assertThat(dlq.received()).hasSize(1);
        assertThat(dlq.received().get(0).getPayload()).contains(unknownBookingId);
    }
}

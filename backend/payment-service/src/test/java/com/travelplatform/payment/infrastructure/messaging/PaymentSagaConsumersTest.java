package com.travelplatform.payment.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises both legs of the choreography saga (ADR 0010) end to end against a real MongoDB
 * (Quarkus Dev Services): booking-created authorizes a payment, booking-cancelled then refunds it.
 * Also covers idempotency (duplicate booking-created) and the retry path (a refund request for a
 * booking with no payment yet).
 */
@QuarkusTest
@DisplayName("Payment saga consumers")
class PaymentSagaConsumersTest {

    @Inject @Any InMemoryConnector connector;
    @Inject MongoClient mongoClient;

    private String bookingCreatedPayload(String bookingId, String amount, String currency) {
        return """
                {"bookingId":{"value":"%s"},"travelerId":{"value":"%s"},\
                "reference":{"itemType":"FLIGHT","itemId":"%s","quantity":1},\
                "amount":{"amount":%s,"currency":"%s"},"occurredOn":"%s"}"""
                .formatted(
                        bookingId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        amount,
                        currency,
                        Instant.now());
    }

    private String bookingCancelledPayload(String bookingId) {
        return """
                {"bookingId":{"value":"%s"},"travelerId":{"value":"%s"},"occurredOn":"%s"}"""
                .formatted(bookingId, UUID.randomUUID(), Instant.now());
    }

    private String paymentStatus(String bookingId) {
        var doc =
                mongoClient
                        .getDatabase("payment")
                        .getCollection("payments")
                        .find(Filters.eq("bookingId", bookingId))
                        .first();
        return doc == null ? null : doc.getString("status");
    }

    @Test
    @DisplayName("authorizes a payment on booking-created, then refunds it on booking-cancelled")
    void authorizesThenRefundsAPayment() {
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> bookingCreated = connector.source("booking-created");
        InMemorySource<String> bookingCancelled = connector.source("booking-cancelled");

        bookingCreated.send(bookingCreatedPayload(bookingId, "450.00", "EUR"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(paymentStatus(bookingId)).isEqualTo("AUTHORIZED"));

        bookingCancelled.send(bookingCancelledPayload(bookingId));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(paymentStatus(bookingId)).isEqualTo("REFUNDED"));
    }

    @Test
    @DisplayName("a duplicate booking-created delivery only creates one payment")
    void duplicateBookingCreatedIsAppliedOnlyOnce() {
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> bookingCreated = connector.source("booking-created");

        bookingCreated.send(bookingCreatedPayload(bookingId, "100.00", "EUR"));
        bookingCreated.send(bookingCreatedPayload(bookingId, "100.00", "EUR"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("payment")
                                            .getCollection("processed_bookings")
                                            .find(Filters.eq("_id", bookingId))
                                            .first();
                            assertThat(doc).isNotNull();
                        });

        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            var count =
                                    mongoClient
                                            .getDatabase("payment")
                                            .getCollection("payments")
                                            .countDocuments(Filters.eq("bookingId", bookingId));
                            assertThat(count).isEqualTo(1);
                        });
    }

    @Test
    @DisplayName(
            "refunding a booking with no payment yet schedules a retry instead of failing silently")
    void refundingAnUnknownBookingSchedulesARetry() {
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> bookingCancelled = connector.source("booking-cancelled");

        bookingCancelled.send(bookingCancelledPayload(bookingId));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("payment")
                                            .getCollection("retry_tasks")
                                            .find(Filters.eq("bookingId", bookingId))
                                            .first();
                            assertThat(doc).isNotNull();
                            assertThat(doc.getString("action")).isEqualTo("booking-cancelled");
                        });
    }

    @Test
    @DisplayName("drops a malformed booking-created payload instead of scheduling a retry for it")
    void dropsAMalformedBookingCreatedPayload() {
        InMemorySource<String> source = connector.source("booking-created");
        var retryTasks = mongoClient.getDatabase("payment").getCollection("retry_tasks");
        var countBefore = retryTasks.countDocuments();

        source.send("{ this is not valid json");

        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(retryTasks.countDocuments()).isEqualTo(countBefore));
    }
}

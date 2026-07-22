package com.travelplatform.booking.infrastructure.messaging;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.travelplatform.booking.api.dto.CreateBookingRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link PaymentAuthorizedConsumer} and {@link PaymentFailedConsumer} end to end: a
 * booking is created over HTTP (real MongoDB, Quarkus Dev Services), then a payment-outcome event
 * arrives on the in-memory channel (standing in for Kafka) and the booking's status changes
 * accordingly - the second leg of the saga documented in docs/adr/0010-payment-saga.md.
 */
@QuarkusTest
@DisplayName("Payment outcome consumers")
class PaymentOutcomeConsumersTest {

    @Inject @Any InMemoryConnector connector;
    @Inject MongoClient mongoClient;

    private String createBooking() {
        return given().contentType("application/json")
                .body(
                        new CreateBookingRequest(
                                UUID.randomUUID().toString(),
                                "FLIGHT",
                                UUID.randomUUID().toString(),
                                1,
                                new BigDecimal("450.00"),
                                "EUR"))
                .post("/api/v1/bookings")
                .then()
                .extract()
                .path("bookingId");
    }

    private String paymentOutcomePayload(String bookingId) {
        return """
                {"paymentId":{"value":"%s"},"bookingId":{"value":"%s"},\
                "amount":{"amount":450.00,"currency":"EUR"},"occurredOn":"%s"}"""
                .formatted(UUID.randomUUID(), bookingId, Instant.now());
    }

    private String bookingStatus(String bookingId) {
        var doc =
                mongoClient
                        .getDatabase("booking")
                        .getCollection("bookings")
                        .find(Filters.eq("_id", bookingId))
                        .first();
        return doc == null ? null : doc.getString("status");
    }

    @Test
    @DisplayName("payment-authorized confirms a pending booking")
    void paymentAuthorizedConfirmsTheBooking() {
        var bookingId = createBooking();
        InMemorySource<String> source = connector.source("payment-authorized");

        source.send(paymentOutcomePayload(bookingId));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(bookingStatus(bookingId)).isEqualTo("CONFIRMED"));
    }

    @Test
    @DisplayName("payment-failed cancels the booking (compensating transaction)")
    void paymentFailedCancelsTheBooking() {
        var bookingId = createBooking();
        InMemorySource<String> source = connector.source("payment-failed");

        source.send(paymentOutcomePayload(bookingId));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(bookingStatus(bookingId)).isEqualTo("CANCELLED"));
    }

    @Test
    @DisplayName("a duplicate payment-authorized delivery is idempotent")
    void duplicatePaymentAuthorizedIsIdempotent() {
        var bookingId = createBooking();
        InMemorySource<String> source = connector.source("payment-authorized");

        source.send(paymentOutcomePayload(bookingId));
        source.send(paymentOutcomePayload(bookingId));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(bookingStatus(bookingId)).isEqualTo("CONFIRMED"));
    }

    @Test
    @DisplayName("a duplicate payment-failed delivery is idempotent")
    void duplicatePaymentFailedIsIdempotent() {
        var bookingId = createBooking();
        InMemorySource<String> source = connector.source("payment-failed");

        source.send(paymentOutcomePayload(bookingId));
        source.send(paymentOutcomePayload(bookingId));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(bookingStatus(bookingId)).isEqualTo("CANCELLED"));
    }

    @Test
    @DisplayName(
            "drops a malformed payment-authorized payload instead of scheduling a retry for it")
    void dropsAMalformedPaymentAuthorizedPayload() {
        InMemorySource<String> source = connector.source("payment-authorized");
        var retryTasks = mongoClient.getDatabase("booking").getCollection("retry_tasks");
        var countBefore = retryTasks.countDocuments();

        source.send("{ this is not valid json");

        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(retryTasks.countDocuments()).isEqualTo(countBefore));
    }
}

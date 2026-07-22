package com.travelplatform.notification.infrastructure.messaging;

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
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@code booking-confirmed} end to end against a real MongoDB (Quarkus Dev Services): a
 * valid event sends the confirmation email and persists a SENT notification. Also covers
 * idempotency (duplicate delivery) and dropping a malformed payload - mirrors payment-service's
 * PaymentSagaConsumersTest.
 */
@QuarkusTest
@DisplayName("BookingConfirmedConsumer")
class BookingConfirmedConsumerTest {

    @Inject @Any InMemoryConnector connector;
    @Inject MongoClient mongoClient;

    private String bookingConfirmedPayload(String bookingId, String travelerEmail) {
        return """
                {"bookingId":{"value":"%s"},"travelerId":{"value":"%s"},\
                "travelerEmail":{"value":"%s"},"occurredOn":"%s"}"""
                .formatted(bookingId, UUID.randomUUID(), travelerEmail, java.time.Instant.now());
    }

    @Test
    @DisplayName("sends the confirmation email and stores a SENT notification for a valid event")
    void sendsConfirmationAndPersistsNotification() {
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-confirmed");

        source.send(bookingConfirmedPayload(bookingId, "traveler@example.com"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("notification")
                                            .getCollection("notifications")
                                            .find(Filters.eq("bookingId", bookingId))
                                            .first();
                            assertThat(doc).isNotNull();
                            assertThat(doc.getString("status")).isEqualTo("SENT");
                            assertThat(doc.getString("recipient"))
                                    .isEqualTo("traveler@example.com");
                        });
    }

    @Test
    @DisplayName("a duplicate booking-confirmed delivery only stores one notification")
    void duplicateDeliveryIsAppliedOnlyOnce() {
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-confirmed");

        source.send(bookingConfirmedPayload(bookingId, "traveler@example.com"));
        source.send(bookingConfirmedPayload(bookingId, "traveler@example.com"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("notification")
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
                                            .getDatabase("notification")
                                            .getCollection("notifications")
                                            .countDocuments(Filters.eq("bookingId", bookingId));
                            assertThat(count).isEqualTo(1);
                        });
    }

    @Test
    @DisplayName("drops a malformed booking-confirmed payload instead of scheduling a retry for it")
    void dropsAMalformedPayload() {
        InMemorySource<String> source = connector.source("booking-confirmed");
        var retryTasks = mongoClient.getDatabase("notification").getCollection("retry_tasks");
        var countBefore = retryTasks.countDocuments();

        source.send("{ this is not valid json");

        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(retryTasks.countDocuments()).isEqualTo(countBefore));
    }
}

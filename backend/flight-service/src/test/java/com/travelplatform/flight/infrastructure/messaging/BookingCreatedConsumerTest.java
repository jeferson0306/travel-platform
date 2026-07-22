package com.travelplatform.flight.infrastructure.messaging;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.travelplatform.flight.api.dto.CreateFlightRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link BookingCreatedConsumer} end to end: a booking-created payload arrives on the
 * in-memory "booking-created" channel (standing in for Kafka - see application.yml's %test profile)
 * and the referenced flight's availableSeats is decremented in a real MongoDB (Quarkus Dev
 * Services). Also covers the two failure paths a real consumer group must handle per ADR 0004:
 * duplicate delivery (idempotency) and insufficient inventory (retry_tasks).
 */
@QuarkusTest
class BookingCreatedConsumerTest {

    @Inject @Any InMemoryConnector connector;
    @Inject MongoClient mongoClient;

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

    private String bookingCreatedPayload(String bookingId, String flightId, int quantity) {
        return """
                {"bookingId":{"value":"%s"},"travelerId":{"value":"%s"},\
                "reference":{"itemType":"FLIGHT","itemId":"%s","quantity":%d},\
                "occurredOn":"%s"}"""
                .formatted(bookingId, UUID.randomUUID(), flightId, quantity, Instant.now());
    }

    @Test
    void decrementsAvailableSeatsOnBookingCreated() {
        var flightId = createFlight(50);
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-created");

        source.send(bookingCreatedPayload(bookingId, flightId, 3));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("flight")
                                            .getCollection("flights")
                                            .find(Filters.eq("_id", flightId))
                                            .first();
                            assertThat(doc).isNotNull();
                            assertThat(doc.getInteger("availableSeats")).isEqualTo(47);
                        });
    }

    @Test
    void sameBookingIdIsOnlyAppliedOnce() {
        var flightId = createFlight(10);
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-created");

        source.send(bookingCreatedPayload(bookingId, flightId, 2));
        source.send(bookingCreatedPayload(bookingId, flightId, 2));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("flight")
                                            .getCollection("processed_bookings")
                                            .find(Filters.eq("_id", bookingId))
                                            .first();
                            assertThat(doc).isNotNull();
                        });

        // Give a possible (incorrect) second decrement a moment to happen before asserting.
        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("flight")
                                            .getCollection("flights")
                                            .find(Filters.eq("_id", flightId))
                                            .first();
                            assertThat(doc.getInteger("availableSeats")).isEqualTo(8);
                        });
    }

    @Test
    void insufficientSeatsSchedulesARetryInsteadOfFailingSilently() {
        var flightId = createFlight(1);
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-created");

        source.send(bookingCreatedPayload(bookingId, flightId, 5));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("flight")
                                            .getCollection("retry_tasks")
                                            .find(Filters.eq("bookingId", bookingId))
                                            .first();
                            assertThat(doc).isNotNull();
                            assertThat(doc.getString("itemId")).isEqualTo(flightId);
                        });
    }
}

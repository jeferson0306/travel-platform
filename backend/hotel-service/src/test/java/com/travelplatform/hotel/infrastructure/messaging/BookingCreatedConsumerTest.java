package com.travelplatform.hotel.infrastructure.messaging;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.travelplatform.hotel.api.dto.CreateHotelRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
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
 * Exercises {@link BookingCreatedConsumer} end to end - see flight-service's equivalent test for
 * the full rationale (idempotency, retry_tasks on failure).
 */
@QuarkusTest
@DisplayName("BookingCreatedConsumer (hotel-inventory)")
class BookingCreatedConsumerTest {

    @Inject @Any InMemoryConnector connector;
    @Inject MongoClient mongoClient;

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

    private String bookingCreatedPayload(
            String bookingId, String itemType, String itemId, int quantity) {
        return """
                {"bookingId":{"value":"%s"},"travelerId":{"value":"%s"},\
                "reference":{"itemType":"%s","itemId":"%s","quantity":%d},\
                "occurredOn":"%s"}"""
                .formatted(bookingId, UUID.randomUUID(), itemType, itemId, quantity, Instant.now());
    }

    private String bookingCreatedPayload(String bookingId, String hotelId, int quantity) {
        return bookingCreatedPayload(bookingId, "HOTEL", hotelId, quantity);
    }

    @Test
    @DisplayName("decrements availableRooms when a HOTEL booking-created event arrives")
    void decrementsAvailableRoomsOnBookingCreated() {
        var hotelId = createHotel(20);
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-created");

        source.send(bookingCreatedPayload(bookingId, hotelId, 3));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("hotel")
                                            .getCollection("hotels")
                                            .find(Filters.eq("_id", hotelId))
                                            .first();
                            assertThat(doc).isNotNull();
                            assertThat(doc.getInteger("availableRooms")).isEqualTo(17);
                        });
    }

    @Test
    @DisplayName("applies the same bookingId only once even if delivered twice")
    void sameBookingIdIsOnlyAppliedOnce() {
        var hotelId = createHotel(10);
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-created");

        source.send(bookingCreatedPayload(bookingId, hotelId, 2));
        source.send(bookingCreatedPayload(bookingId, hotelId, 2));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("hotel")
                                            .getCollection("processed_bookings")
                                            .find(Filters.eq("_id", bookingId))
                                            .first();
                            assertThat(doc).isNotNull();
                        });

        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("hotel")
                                            .getCollection("hotels")
                                            .find(Filters.eq("_id", hotelId))
                                            .first();
                            assertThat(doc.getInteger("availableRooms")).isEqualTo(8);
                        });
    }

    @Test
    @DisplayName("schedules a retry instead of failing silently when there aren't enough rooms")
    void insufficientRoomsSchedulesARetryInsteadOfFailingSilently() {
        var hotelId = createHotel(1);
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-created");

        source.send(bookingCreatedPayload(bookingId, hotelId, 5));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var doc =
                                    mongoClient
                                            .getDatabase("hotel")
                                            .getCollection("retry_tasks")
                                            .find(Filters.eq("bookingId", bookingId))
                                            .first();
                            assertThat(doc).isNotNull();
                            assertThat(doc.getString("itemId")).isEqualTo(hotelId);
                        });
    }

    @Test
    @DisplayName("ignores a FLIGHT-referenced booking-created event entirely")
    void ignoresAFlightReferencedEvent() {
        var hotelId = createHotel(10);
        var bookingId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("booking-created");

        source.send(bookingCreatedPayload(bookingId, "FLIGHT", UUID.randomUUID().toString(), 2));

        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            var processed =
                                    mongoClient
                                            .getDatabase("hotel")
                                            .getCollection("processed_bookings")
                                            .find(Filters.eq("_id", bookingId))
                                            .first();
                            assertThat(processed).isNull();
                            var hotel =
                                    mongoClient
                                            .getDatabase("hotel")
                                            .getCollection("hotels")
                                            .find(Filters.eq("_id", hotelId))
                                            .first();
                            assertThat(hotel.getInteger("availableRooms")).isEqualTo(10);
                        });
    }

    @Test
    @DisplayName("drops an unparseable payload instead of scheduling a retry for it")
    void dropsAMalformedPayloadWithoutRetrying() {
        InMemorySource<String> source = connector.source("booking-created");
        var retryTasks = mongoClient.getDatabase("hotel").getCollection("retry_tasks");
        var countBefore = retryTasks.countDocuments();

        source.send("{ this is not valid json");

        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(retryTasks.countDocuments()).isEqualTo(countBefore));
    }
}

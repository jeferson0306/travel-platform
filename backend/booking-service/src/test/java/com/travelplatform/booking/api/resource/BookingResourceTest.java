package com.travelplatform.booking.api.resource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.travelplatform.booking.api.dto.CreateBookingRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises booking creation and cancellation end to end over HTTP against a real MongoDB (Quarkus
 * Dev Services, replica-set-enabled so the outbox transaction actually runs), then awaits the
 * OutboxRelay's scheduled poll and asserts the event lands on the right Kafka topic - captured via
 * the in-memory connector instead of a real broker, see application.yml's %test profile.
 */
@QuarkusTest
class BookingResourceTest {

    @Inject @Any InMemoryConnector connector;

    private InMemorySink<String> bookingEvents;

    @BeforeEach
    void setUp() {
        bookingEvents = connector.sink("booking-events");
        bookingEvents.clear();
    }

    @Test
    void createsABookingAndPublishesBookingCreated() {
        var travelerId = UUID.randomUUID().toString();

        var bookingId =
                given().contentType("application/json")
                        .body(new CreateBookingRequest(travelerId, "flight-LIS-GRU-2026-08-01"))
                        .when()
                        .post("/api/v1/bookings")
                        .then()
                        .statusCode(201)
                        .body("bookingId", org.hamcrest.Matchers.notNullValue())
                        .extract()
                        .path("bookingId")
                        .toString();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var message =
                                    bookingEvents.received().stream()
                                            .filter(m -> m.getPayload().contains(bookingId))
                                            .findFirst();
                            assertThat(message).isPresent();
                            assertThat(message.get().getPayload()).contains(travelerId);
                            var metadata =
                                    message.get().getMetadata(OutgoingKafkaRecordMetadata.class);
                            assertThat(metadata).isPresent();
                            assertThat(metadata.get().getTopic()).isEqualTo("booking-created");
                        });
    }

    @Test
    void cancelsABookingAndPublishesBookingCancelled() {
        var travelerId = UUID.randomUUID().toString();

        var bookingId =
                given().contentType("application/json")
                        .body(new CreateBookingRequest(travelerId, "hotel-lisbon"))
                        .post("/api/v1/bookings")
                        .then()
                        .extract()
                        .path("bookingId")
                        .toString();

        given().when().post("/api/v1/bookings/" + bookingId + "/cancel").then().statusCode(204);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var message =
                                    bookingEvents.received().stream()
                                            .filter(
                                                    m ->
                                                            m.getPayload().contains(bookingId)
                                                                    && m.getMetadata(
                                                                                    OutgoingKafkaRecordMetadata
                                                                                            .class)
                                                                            .map(
                                                                                    OutgoingKafkaRecordMetadata
                                                                                            ::getTopic)
                                                                            .map(
                                                                                    "booking-cancelled"
                                                                                            ::equals)
                                                                            .orElse(false))
                                            .findFirst();
                            assertThat(message).isPresent();
                        });
    }

    @Test
    void cancellingAnUnknownBookingReturnsNotFound() {
        given().when()
                .post("/api/v1/bookings/" + UUID.randomUUID() + "/cancel")
                .then()
                .statusCode(404)
                .body("error", org.hamcrest.Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void rejectsInvalidPayload() {
        given().contentType("application/json")
                .body(new CreateBookingRequest("not-a-uuid", "hotel-lisbon"))
                .when()
                .post("/api/v1/bookings")
                .then()
                .statusCode(400)
                .body("error", org.hamcrest.Matchers.equalTo("VALIDATION_ERROR"));
    }
}

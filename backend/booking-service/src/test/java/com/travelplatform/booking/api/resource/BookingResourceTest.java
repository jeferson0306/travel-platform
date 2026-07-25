package com.travelplatform.booking.api.resource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.travelplatform.booking.api.dto.CreateBookingRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises booking creation and cancellation end to end over HTTP against a real MongoDB (Quarkus
 * Dev Services, replica-set-enabled so the outbox transaction actually runs), then awaits the
 * OutboxRelay's scheduled poll and asserts the event lands on the right Kafka topic - captured via
 * the in-memory connector instead of a real broker, see application.yml's %test profile. Tokens are
 * minted with the test-only private key in src/test/resources/privateKey.pem, mirroring
 * identity-service's real signing key (smallrye-jwt-build, test scope only) - travelerId always
 * comes from the JWT subject, never from request bodies, so every test authenticates as a specific
 * traveler rather than passing an id directly.
 */
@QuarkusTest
@DisplayName("BookingResource")
class BookingResourceTest {

    @Inject @Any InMemoryConnector connector;

    private InMemorySink<String> bookingEvents;

    @BeforeEach
    void setUp() {
        bookingEvents = connector.sink("booking-events");
        bookingEvents.clear();
    }

    private String tokenFor(String travelerId) {
        return Jwt.issuer("travel-platform-identity").subject(travelerId).sign("privateKey.pem");
    }

    @Test
    void createsABookingAndPublishesBookingCreated() {
        var travelerId = UUID.randomUUID().toString();

        var bookingId =
                given().header("Authorization", "Bearer " + tokenFor(travelerId))
                        .contentType("application/json")
                        .body(
                                new CreateBookingRequest(
                                        "traveler@example.com",
                                        "FLIGHT",
                                        UUID.randomUUID().toString(),
                                        2,
                                        new BigDecimal("450.00"),
                                        "EUR",
                                        null))
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
    void creatingWithoutATokenIsRejected() {
        given().contentType("application/json")
                .body(
                        new CreateBookingRequest(
                                "traveler@example.com",
                                "FLIGHT",
                                UUID.randomUUID().toString(),
                                1,
                                new BigDecimal("100.00"),
                                "EUR",
                                null))
                .when()
                .post("/api/v1/bookings")
                .then()
                .statusCode(401);
    }

    @Test
    void cancelsABookingAndPublishesBookingCancelled() {
        var travelerId = UUID.randomUUID().toString();
        var token = tokenFor(travelerId);

        var bookingId =
                given().header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .body(
                                new CreateBookingRequest(
                                        "traveler@example.com",
                                        "HOTEL",
                                        UUID.randomUUID().toString(),
                                        1,
                                        new BigDecimal("95.00"),
                                        "EUR",
                                        null))
                        .post("/api/v1/bookings")
                        .then()
                        .extract()
                        .path("bookingId")
                        .toString();

        given().header("Authorization", "Bearer " + token)
                .when()
                .post("/api/v1/bookings/" + bookingId + "/cancel")
                .then()
                .statusCode(204);

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
    void cancellingSomeoneElsesBookingIsForbidden() {
        var owner = UUID.randomUUID().toString();
        var intruder = UUID.randomUUID().toString();

        var bookingId =
                given().header("Authorization", "Bearer " + tokenFor(owner))
                        .contentType("application/json")
                        .body(
                                new CreateBookingRequest(
                                        "owner@example.com",
                                        "HOTEL",
                                        UUID.randomUUID().toString(),
                                        1,
                                        new BigDecimal("95.00"),
                                        "EUR",
                                        null))
                        .post("/api/v1/bookings")
                        .then()
                        .extract()
                        .path("bookingId")
                        .toString();

        given().header("Authorization", "Bearer " + tokenFor(intruder))
                .when()
                .post("/api/v1/bookings/" + bookingId + "/cancel")
                .then()
                .statusCode(403)
                .body("error", org.hamcrest.Matchers.equalTo("FORBIDDEN"));
    }

    @Test
    void cancellingAnUnknownBookingReturnsNotFound() {
        given().header("Authorization", "Bearer " + tokenFor(UUID.randomUUID().toString()))
                .when()
                .post("/api/v1/bookings/" + UUID.randomUUID() + "/cancel")
                .then()
                .statusCode(404)
                .body("error", org.hamcrest.Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void listsOnlyTheCallersOwnBookings() {
        var travelerId = UUID.randomUUID().toString();
        var otherTravelerId = UUID.randomUUID().toString();

        given().header("Authorization", "Bearer " + tokenFor(travelerId))
                .contentType("application/json")
                .body(
                        new CreateBookingRequest(
                                "traveler@example.com",
                                "FLIGHT",
                                UUID.randomUUID().toString(),
                                1,
                                new BigDecimal("120.00"),
                                "EUR",
                                null))
                .post("/api/v1/bookings")
                .then()
                .statusCode(201);
        given().header("Authorization", "Bearer " + tokenFor(otherTravelerId))
                .contentType("application/json")
                .body(
                        new CreateBookingRequest(
                                "someone-else@example.com",
                                "HOTEL",
                                UUID.randomUUID().toString(),
                                1,
                                new BigDecimal("80.00"),
                                "EUR",
                                null))
                .post("/api/v1/bookings")
                .then()
                .statusCode(201);

        given().header("Authorization", "Bearer " + tokenFor(travelerId))
                .when()
                .get("/api/v1/bookings")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(1))
                .body("[0].itemType", org.hamcrest.Matchers.equalTo("FLIGHT"))
                .body("[0].status", org.hamcrest.Matchers.equalTo("PENDING"));
    }

    @Test
    void listingWithoutATokenIsRejected() {
        given().when().get("/api/v1/bookings").then().statusCode(401);
    }

    @Test
    void createsABookingWithoutAnItemSummaryAndItStaysNullInTheListResponse() {
        var travelerId = UUID.randomUUID().toString();

        given().header("Authorization", "Bearer " + tokenFor(travelerId))
                .contentType("application/json")
                .body(
                        new CreateBookingRequest(
                                "traveler@example.com",
                                "FLIGHT",
                                UUID.randomUUID().toString(),
                                1,
                                new BigDecimal("100.00"),
                                "EUR",
                                null))
                .when()
                .post("/api/v1/bookings")
                .then()
                .statusCode(201);

        given().header("Authorization", "Bearer " + tokenFor(travelerId))
                .when()
                .get("/api/v1/bookings")
                .then()
                .statusCode(200)
                .body("[0].itemSummary", org.hamcrest.Matchers.nullValue());
    }

    @Test
    void createsABookingWithAnItemSummaryAndReturnsItUnchangedInTheListResponse() {
        var travelerId = UUID.randomUUID().toString();
        var itemSummary = "Lisbon -> Sao Paulo, TP123, TAP Air Portugal";

        given().header("Authorization", "Bearer " + tokenFor(travelerId))
                .contentType("application/json")
                .body(
                        new CreateBookingRequest(
                                "traveler@example.com",
                                "FLIGHT",
                                UUID.randomUUID().toString(),
                                1,
                                new BigDecimal("589.00"),
                                "EUR",
                                itemSummary))
                .when()
                .post("/api/v1/bookings")
                .then()
                .statusCode(201);

        given().header("Authorization", "Bearer " + tokenFor(travelerId))
                .when()
                .get("/api/v1/bookings")
                .then()
                .statusCode(200)
                .body("[0].itemSummary", org.hamcrest.Matchers.equalTo(itemSummary));
    }

    @Test
    void rejectsInvalidPayload() {
        given().header("Authorization", "Bearer " + tokenFor(UUID.randomUUID().toString()))
                .contentType("application/json")
                .body(
                        new CreateBookingRequest(
                                "not-an-email",
                                "HOTEL",
                                UUID.randomUUID().toString(),
                                1,
                                new BigDecimal("95.00"),
                                "EUR",
                                null))
                .when()
                .post("/api/v1/bookings")
                .then()
                .statusCode(400)
                .body("error", org.hamcrest.Matchers.equalTo("VALIDATION_ERROR"));
    }
}

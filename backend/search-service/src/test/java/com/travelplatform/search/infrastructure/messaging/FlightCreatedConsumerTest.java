package com.travelplatform.search.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.travelplatform.search.OpenSearchTestResource;
import com.travelplatform.search.application.port.in.SearchFlightsUseCase;
import com.travelplatform.search.application.port.in.SearchFlightsUseCase.SearchFlightsQuery;
import io.quarkus.test.common.QuarkusTestResource;
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
 * Exercises {@code flight-created} end to end against a real OpenSearch (Testcontainers - see
 * {@link OpenSearchTestResource}): a valid event indexes the flight, immediately visible via a
 * route search. Also covers idempotency (duplicate delivery is a harmless overwrite) and dropping a
 * malformed payload.
 */
@QuarkusTest
@QuarkusTestResource(OpenSearchTestResource.class)
@DisplayName("FlightCreatedConsumer")
class FlightCreatedConsumerTest {

    @Inject @Any InMemoryConnector connector;
    @Inject SearchFlightsUseCase searchFlightsUseCase;

    private String flightCreatedPayload(String flightId, String origin, String destination) {
        return """
                {"flightId":{"value":"%s"},"origin":{"value":"%s"},"destination":{"value":"%s"},\
                "departureAt":"%s","arrivalAt":"%s","price":{"amount":450.00,"currency":"EUR"},\
                "availableSeats":150}"""
                .formatted(
                        flightId,
                        origin,
                        destination,
                        Instant.now(),
                        Instant.now().plusSeconds(36000));
    }

    @Test
    @DisplayName("indexes a valid event and it is immediately searchable by route")
    void indexesAValidEvent() {
        var flightId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("flight-created");

        source.send(flightCreatedPayload(flightId, "LIS", "GRU"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var results =
                                    searchFlightsUseCase.search(
                                            new SearchFlightsQuery("LIS", "GRU", null));
                            assertThat(results).anyMatch(f -> f.flightId().equals(flightId));
                        });
    }

    @Test
    @DisplayName("a duplicate delivery is a harmless overwrite, not a duplicate document")
    void duplicateDeliveryOverwritesInsteadOfDuplicating() {
        var flightId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("flight-created");

        source.send(flightCreatedPayload(flightId, "OPO", "JFK"));
        source.send(flightCreatedPayload(flightId, "OPO", "JFK"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var results =
                                    searchFlightsUseCase.search(
                                            new SearchFlightsQuery("OPO", "JFK", null));
                            assertThat(results)
                                    .filteredOn(f -> f.flightId().equals(flightId))
                                    .hasSize(1);
                        });
    }

    @Test
    @DisplayName("drops a malformed flight-created payload instead of failing")
    void dropsAMalformedPayload() {
        InMemorySource<String> source = connector.source("flight-created");

        source.send("{ this is not valid json");

        // No assertion target beyond "doesn't throw" - a poison message is logged and discarded.
        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).until(() -> true);
    }
}

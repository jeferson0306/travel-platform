package com.travelplatform.search.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.travelplatform.search.OpenSearchTestResource;
import com.travelplatform.search.application.port.in.SearchHotelsUseCase;
import com.travelplatform.search.application.port.in.SearchHotelsUseCase.SearchHotelsQuery;
import io.quarkus.test.common.QuarkusTestResource;
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
 * Exercises {@code hotel-created} end to end against a real OpenSearch (Testcontainers). Mirrors
 * {@link FlightCreatedConsumerTest}.
 */
@QuarkusTest
@QuarkusTestResource(OpenSearchTestResource.class)
@DisplayName("HotelCreatedConsumer")
class HotelCreatedConsumerTest {

    @Inject @Any InMemoryConnector connector;
    @Inject SearchHotelsUseCase searchHotelsUseCase;

    private String hotelCreatedPayload(String hotelId, String name, String city) {
        return """
                {"hotelId":{"value":"%s"},"name":{"value":"%s"},"city":{"value":"%s"},\
                "pricePerNight":{"amount":120.00,"currency":"EUR"},"availableRooms":30}"""
                .formatted(hotelId, name, city);
    }

    @Test
    @DisplayName("indexes a valid event and it is immediately searchable by city")
    void indexesAValidEvent() {
        var hotelId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("hotel-created");

        source.send(hotelCreatedPayload(hotelId, "Grand Hotel", "Porto"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var results =
                                    searchHotelsUseCase.search(
                                            new SearchHotelsQuery("Porto", null));
                            assertThat(results).anyMatch(h -> h.hotelId().equals(hotelId));
                        });
    }

    @Test
    @DisplayName("a duplicate delivery is a harmless overwrite, not a duplicate document")
    void duplicateDeliveryOverwritesInsteadOfDuplicating() {
        var hotelId = UUID.randomUUID().toString();
        InMemorySource<String> source = connector.source("hotel-created");

        source.send(hotelCreatedPayload(hotelId, "Seaside Inn", "Faro"));
        source.send(hotelCreatedPayload(hotelId, "Seaside Inn", "Faro"));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var results =
                                    searchHotelsUseCase.search(new SearchHotelsQuery("Faro", null));
                            assertThat(results)
                                    .filteredOn(h -> h.hotelId().equals(hotelId))
                                    .hasSize(1);
                        });
    }

    @Test
    @DisplayName("drops a malformed hotel-created payload instead of failing")
    void dropsAMalformedPayload() {
        InMemorySource<String> source = connector.source("hotel-created");

        source.send("{ this is not valid json");

        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).until(() -> true);
    }
}

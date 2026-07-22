package com.travelplatform.search.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.travelplatform.search.OpenSearchTestResource;
import com.travelplatform.search.application.port.in.SearchFlightsUseCase;
import com.travelplatform.search.application.port.in.SearchFlightsUseCase.SearchFlightsQuery;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link RetryRelay#relay()} directly (package-visible, called synchronously here instead of
 * waiting on its real 10s @Scheduled trigger) - see flight-service's equivalent test for the full
 * rationale. A genuinely malformed payload (not a business failure - this service has none, since
 * indexing never rejects valid data) is used to force a deterministic, repeatable failure for the
 * backoff/dead-letter tests. Backed-off tasks are fast-forwarded via {@link
 * RetryTaskStore#findAll()} instead of waiting out the real exponential delay.
 */
@QuarkusTest
@QuarkusTestResource(OpenSearchTestResource.class)
@DisplayName("RetryRelay")
class RetryRelayTest {

    private static final String MALFORMED_PAYLOAD = "{ not valid json";

    @Inject RetryRelay retryRelay;
    @Inject RetryTaskStore retryTaskStore;
    @Inject SearchFlightsUseCase searchFlightsUseCase;
    @Inject @Any InMemoryConnector connector;

    @BeforeEach
    void setUp() {
        connector.sink("flight-created-dlq").clear();
    }

    private String validFlightPayload(String flightId) {
        return """
                {"flightId":{"value":"%s"},"origin":{"value":"LIS"},"destination":{"value":"GRU"},\
                "departureAt":"%s","arrivalAt":"%s","price":{"amount":450.00,"currency":"EUR"},\
                "availableSeats":150}"""
                .formatted(flightId, Instant.now(), Instant.now().plusSeconds(36000));
    }

    /**
     * Resets every currently-tracked retry task's {@code nextAttemptAt} to "now", so the next
     * {@code relay()} call picks it up immediately regardless of how far backoff pushed it out.
     */
    private void forceAllTasksDue() {
        for (var task : retryTaskStore.findAll()) {
            task.document().nextAttemptAt = Instant.now().minusSeconds(1);
            retryTaskStore.reschedule(task.id(), task.document());
        }
    }

    @Test
    @DisplayName("a retry that now succeeds indexes the flight and clears the task")
    void successfulRetryClearsTheTask() {
        var flightId = UUID.randomUUID().toString();
        retryTaskStore.schedule("flight-created", validFlightPayload(flightId), "seeded by test");

        retryRelay.relay();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var results =
                                    searchFlightsUseCase.search(
                                            new SearchFlightsQuery("LIS", "GRU", null));
                            assertThat(results).anyMatch(f -> f.flightId().equals(flightId));
                        });
        assertThat(retryTaskStore.findAll())
                .noneMatch(t -> t.document().payload.contains(flightId));
    }

    @Test
    @DisplayName("a retry that still fails increments attempts and backs off exponentially")
    void failedRetryBacksOffExponentially() {
        retryTaskStore.schedule("flight-created", MALFORMED_PAYLOAD, "seeded by test");
        var before = Instant.now();

        retryRelay.relay();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            var task = retryTaskStore.findAll().stream().findFirst();
                            assertThat(task).isPresent();
                            assertThat(task.get().document().attempts).isEqualTo(1);
                            assertThat(task.get().document().nextAttemptAt)
                                    .isAfter(before.plusSeconds(19))
                                    .isBefore(before.plusSeconds(25));
                        });
    }

    @Test
    @DisplayName(
            "exhausting all attempts dead-letters the task and publishes to the matching DLQ topic")
    void exhaustingRetriesMovesToDeadLettersAndPublishesToDlq() {
        retryTaskStore.schedule("flight-created", MALFORMED_PAYLOAD, "seeded by test");

        // MAX_ATTEMPTS is 5 - drive five relay() passes, fast-forwarding backoff between each.
        for (int i = 0; i < 5; i++) {
            forceAllTasksDue();
            retryRelay.relay();
        }

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(retryTaskStore.findAll()).isEmpty());
        InMemorySink<String> dlq = connector.sink("flight-created-dlq");
        assertThat(dlq.received()).hasSize(1);
    }
}

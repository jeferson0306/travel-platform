package com.travelplatform.search.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.search.application.port.in.IndexFlightUseCase;
import com.travelplatform.search.application.port.in.IndexFlightUseCase.IndexFlightCommand;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code flight-created} (own consumer group "search-indexer" - see application.yml) and
 * indexes the flight for search.
 *
 * <p>Idempotency: unlike every Mongo-backed consumer in this platform, no {@code
 * processed_bookings} claim collection is needed - indexing by {@code flightId} is an upsert, so a
 * duplicate delivery just overwrites the same document with identical content. See
 * docs/adr/0012-search-service-opensearch.md.
 *
 * <p>Failure handling: a poison (unparseable) message is logged and dropped - retrying it can never
 * succeed. A technical failure (OpenSearch unavailable) is recorded in the "retry_tasks" index and
 * retried by {@link RetryRelay} with backoff, moving to "dead_letters" after too many attempts -
 * same shape as every other consumer, backed by OpenSearch instead of MongoDB here.
 */
@ApplicationScoped
public class FlightCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(FlightCreatedConsumer.class);

    private final ObjectMapper objectMapper;
    private final IndexFlightUseCase indexFlightUseCase;
    private final RetryTaskStore retryTaskStore;

    public FlightCreatedConsumer(
            ObjectMapper objectMapper,
            IndexFlightUseCase indexFlightUseCase,
            RetryTaskStore retryTaskStore) {
        this.objectMapper = objectMapper;
        this.indexFlightUseCase = indexFlightUseCase;
        this.retryTaskStore = retryTaskStore;
    }

    @Incoming("flight-created")
    public void onMessage(String payload) {
        FlightCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, FlightCreatedEvent.class);
        } catch (Exception e) {
            LOG.error("Malformed flight-created payload, dropping: " + payload, e);
            return;
        }

        try {
            indexFlightUseCase.index(toCommand(event));
        } catch (RuntimeException e) {
            LOG.error("Failed to index flight " + event.flightId().value(), e);
            retryTaskStore.schedule("flight-created", payload, e.getMessage());
        }
    }

    private IndexFlightCommand toCommand(FlightCreatedEvent event) {
        return new IndexFlightCommand(
                event.flightId().value(),
                event.origin().value(),
                event.destination().value(),
                event.departureAt(),
                event.arrivalAt(),
                event.price().amount(),
                event.price().currency(),
                event.availableSeats());
    }
}

package com.travelplatform.search.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.search.application.port.in.IndexHotelUseCase;
import com.travelplatform.search.application.port.in.IndexHotelUseCase.IndexHotelCommand;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code hotel-created} (own consumer group "search-indexer" - see application.yml) and
 * indexes the hotel for search. Same idempotency-for-free reasoning and failure handling as {@link
 * FlightCreatedConsumer}.
 */
@ApplicationScoped
public class HotelCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(HotelCreatedConsumer.class);

    private final ObjectMapper objectMapper;
    private final IndexHotelUseCase indexHotelUseCase;
    private final RetryTaskStore retryTaskStore;

    public HotelCreatedConsumer(
            ObjectMapper objectMapper,
            IndexHotelUseCase indexHotelUseCase,
            RetryTaskStore retryTaskStore) {
        this.objectMapper = objectMapper;
        this.indexHotelUseCase = indexHotelUseCase;
        this.retryTaskStore = retryTaskStore;
    }

    @Incoming("hotel-created")
    public void onMessage(String payload) {
        HotelCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, HotelCreatedEvent.class);
        } catch (Exception e) {
            LOG.error("Malformed hotel-created payload, dropping: " + payload, e);
            return;
        }

        try {
            indexHotelUseCase.index(toCommand(event));
        } catch (RuntimeException e) {
            LOG.error("Failed to index hotel " + event.hotelId().value(), e);
            retryTaskStore.schedule("hotel-created", payload, e.getMessage());
        }
    }

    private IndexHotelCommand toCommand(HotelCreatedEvent event) {
        return new IndexHotelCommand(
                event.hotelId().value(),
                event.name().value(),
                event.city().value(),
                event.pricePerNight().amount(),
                event.pricePerNight().currency(),
                event.availableRooms());
    }
}

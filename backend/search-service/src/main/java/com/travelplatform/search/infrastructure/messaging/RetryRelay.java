package com.travelplatform.search.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.search.application.port.in.IndexFlightUseCase;
import com.travelplatform.search.application.port.in.IndexHotelUseCase;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

/**
 * Retries "retry_tasks" left behind by {@link FlightCreatedConsumer}/{@link HotelCreatedConsumer}
 * with exponential backoff, dispatching on the task's {@code eventType} - the same shape
 * payment-service's RetryRelay dispatches on {@code action}, just with two index operations instead
 * of authorize/refund. After {@link #MAX_ATTEMPTS} failed attempts, the task is moved to
 * "dead_letters" and a summary is published to the matching DLQ Kafka topic. Backed by OpenSearch
 * instead of MongoDB - see docs/adr/0012-search-service-opensearch.md.
 */
@ApplicationScoped
public class RetryRelay {

    private static final Logger LOG = Logger.getLogger(RetryRelay.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_BACKOFF_SECONDS = 10;

    private final RetryTaskStore retryTaskStore;
    private final ObjectMapper objectMapper;
    private final IndexFlightUseCase indexFlightUseCase;
    private final IndexHotelUseCase indexHotelUseCase;
    private final Emitter<String> flightCreatedDlqEmitter;
    private final Emitter<String> hotelCreatedDlqEmitter;

    public RetryRelay(
            RetryTaskStore retryTaskStore,
            ObjectMapper objectMapper,
            IndexFlightUseCase indexFlightUseCase,
            IndexHotelUseCase indexHotelUseCase,
            @Channel("flight-created-dlq") Emitter<String> flightCreatedDlqEmitter,
            @Channel("hotel-created-dlq") Emitter<String> hotelCreatedDlqEmitter) {
        this.retryTaskStore = retryTaskStore;
        this.objectMapper = objectMapper;
        this.indexFlightUseCase = indexFlightUseCase;
        this.indexHotelUseCase = indexHotelUseCase;
        this.flightCreatedDlqEmitter = flightCreatedDlqEmitter;
        this.hotelCreatedDlqEmitter = hotelCreatedDlqEmitter;
    }

    @Scheduled(every = "10s")
    void relay() {
        for (var task : retryTaskStore.findDue()) {
            attempt(task);
        }
    }

    private void attempt(RetryTaskStore.RetryTask task) {
        var id = task.id();
        var document = task.document();
        var attempts = document.attempts + 1;

        boolean succeeded;
        try {
            reindex(document.eventType, document.payload);
            succeeded = true;
        } catch (Exception e) {
            LOG.error("Retry attempt " + attempts + " failed for task " + id, e);
            document.lastError = e.getMessage();
            succeeded = false;
        }

        if (succeeded) {
            retryTaskStore.delete(id);
            return;
        }

        if (attempts >= MAX_ATTEMPTS) {
            document.attempts = attempts;
            retryTaskStore.deadLetter(id, document);
            LOG.error(
                    document.eventType
                            + " task "
                            + id
                            + " moved to DLQ after "
                            + attempts
                            + " attempts");
            publishToDlq(document.eventType, id, attempts);
            return;
        }

        var backoffSeconds = BASE_BACKOFF_SECONDS * (1L << attempts);
        document.attempts = attempts;
        document.nextAttemptAt = Instant.now().plusSeconds(backoffSeconds);
        retryTaskStore.reschedule(id, document);
    }

    private void reindex(String eventType, String payload) throws Exception {
        switch (eventType) {
            case "flight-created" -> {
                var event = objectMapper.readValue(payload, FlightCreatedEvent.class);
                indexFlightUseCase.index(
                        new IndexFlightUseCase.IndexFlightCommand(
                                event.flightId().value(),
                                event.origin().value(),
                                event.destination().value(),
                                event.departureAt(),
                                event.arrivalAt(),
                                event.price().amount(),
                                event.price().currency(),
                                event.availableSeats()));
            }
            case "hotel-created" -> {
                var event = objectMapper.readValue(payload, HotelCreatedEvent.class);
                indexHotelUseCase.index(
                        new IndexHotelUseCase.IndexHotelCommand(
                                event.hotelId().value(),
                                event.name().value(),
                                event.city().value(),
                                event.pricePerNight().amount(),
                                event.pricePerNight().currency(),
                                event.availableRooms()));
            }
            default ->
                    throw new IllegalStateException("Unknown retry task eventType: " + eventType);
        }
    }

    private void publishToDlq(String eventType, String id, int attempts) {
        var message = "{\"taskId\":\"" + id + "\",\"attempts\":" + attempts + "}";
        if ("flight-created".equals(eventType)) {
            flightCreatedDlqEmitter.send(message);
        } else {
            hotelCreatedDlqEmitter.send(message);
        }
    }
}

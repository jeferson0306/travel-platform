package com.travelplatform.flight.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.travelplatform.flight.application.port.in.ReserveInventoryUseCase;
import com.travelplatform.flight.application.port.in.ReserveInventoryUseCase.ReserveInventoryCommand;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code booking-created} (own consumer group "flight-inventory" - see application.yml)
 * and decrements the referenced flight's available seats. Only reacts to bookings whose reference
 * is a FLIGHT; HOTEL bookings are silently skipped (hotel-service has its own consumer group on the
 * same topic - ADR 0004).
 *
 * <p>Idempotency: a {@code processed_bookings} collection, keyed by bookingId, is claimed before
 * any inventory mutation - Kafka's at-least-once delivery means the same message can arrive twice.
 *
 * <p>Failure handling: a poison (unparseable) message is logged and dropped - retrying it can never
 * succeed. A processing failure (Mongo error, insufficient seats) does not nack the message (Kafka
 * has no delayed redelivery); instead it is recorded in a {@code retry_tasks} collection and
 * retried by {@link RetryRelay} with backoff, moving to a DLQ after too many attempts - see the ADR
 * 0004 addendum for why this is Mongo-backed rather than a literal second Kafka topic.
 */
@ApplicationScoped
public class BookingCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(BookingCreatedConsumer.class);
    private static final String ITEM_TYPE = "FLIGHT";

    private final ObjectMapper objectMapper;
    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final MongoCollection<Document> processedBookings;
    private final MongoCollection<Document> retryTasks;

    public BookingCreatedConsumer(
            ObjectMapper objectMapper,
            ReserveInventoryUseCase reserveInventoryUseCase,
            MongoClient mongoClient,
            @ConfigProperty(name = "flight.mongo.database", defaultValue = "flight")
                    String database) {
        this.objectMapper = objectMapper;
        this.reserveInventoryUseCase = reserveInventoryUseCase;
        var db = mongoClient.getDatabase(database);
        this.processedBookings = db.getCollection("processed_bookings");
        this.retryTasks = db.getCollection("retry_tasks");
    }

    @Incoming("booking-created")
    public void onMessage(String payload) {
        BookingCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, BookingCreatedEvent.class);
        } catch (Exception e) {
            LOG.error("Malformed booking-created payload, dropping: " + payload, e);
            return;
        }

        if (event.reference() == null || !ITEM_TYPE.equals(event.reference().itemType())) {
            return;
        }

        process(
                event.bookingId().value(),
                event.reference().itemId(),
                event.reference().quantity());
    }

    private void process(String bookingId, String itemId, int quantity) {
        if (!claim(bookingId)) {
            return;
        }

        boolean reserved;
        try {
            reserved =
                    reserveInventoryUseCase.reserve(new ReserveInventoryCommand(itemId, quantity));
        } catch (RuntimeException e) {
            LOG.error("Failed to reserve inventory for booking " + bookingId, e);
            scheduleRetry(bookingId, itemId, quantity, e.getMessage());
            return;
        }

        if (!reserved) {
            scheduleRetry(bookingId, itemId, quantity, "flight not found or insufficient seats");
        }
    }

    private boolean claim(String bookingId) {
        try {
            processedBookings.insertOne(
                    new Document("_id", bookingId).append("processedAt", Date.from(Instant.now())));
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    private void scheduleRetry(String bookingId, String itemId, int quantity, String reason) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("bookingId", bookingId)
                        .append("itemId", itemId)
                        .append("quantity", quantity)
                        .append("attempts", 0)
                        .append("lastError", reason)
                        .append("nextAttemptAt", Date.from(Instant.now())));
    }
}

package com.travelplatform.hotel.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.travelplatform.hotel.application.port.in.ReserveInventoryUseCase;
import com.travelplatform.hotel.application.port.in.ReserveInventoryUseCase.ReserveInventoryCommand;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code booking-created} (own consumer group "hotel-inventory" - see application.yml) and
 * decrements the referenced hotel's available rooms. Only reacts to bookings whose reference is a
 * HOTEL; FLIGHT bookings are silently skipped (flight-service has its own consumer group on the
 * same topic - ADR 0004). See flight-service's equivalent class for the full rationale.
 */
@ApplicationScoped
public class BookingCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(BookingCreatedConsumer.class);
    private static final String ITEM_TYPE = "HOTEL";

    private final ObjectMapper objectMapper;
    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final MongoCollection<Document> processedBookings;
    private final MongoCollection<Document> retryTasks;

    public BookingCreatedConsumer(
            ObjectMapper objectMapper,
            ReserveInventoryUseCase reserveInventoryUseCase,
            MongoClient mongoClient,
            @ConfigProperty(name = "hotel.mongo.database", defaultValue = "hotel")
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
            scheduleRetry(bookingId, itemId, quantity, "hotel not found or insufficient rooms");
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

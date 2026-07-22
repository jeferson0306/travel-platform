package com.travelplatform.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.travelplatform.notification.application.port.in.SendBookingConfirmationUseCase;
import com.travelplatform.notification.application.port.in.SendBookingConfirmationUseCase.SendBookingConfirmationCommand;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code booking-confirmed} (own consumer group "notification-processor" - see
 * application.yml) and sends the confirmation email.
 *
 * <p>Idempotency: a {@code processed_bookings} collection, keyed by bookingId, is claimed before
 * sending - Kafka's at-least-once delivery means the same message can arrive twice, and resending
 * would spam the traveler.
 *
 * <p>Failure handling: a poison (unparseable) message is logged and dropped - retrying it can never
 * succeed. A technical failure (email gateway unavailable, Mongo error) does not nack the message
 * (Kafka has no delayed redelivery); instead it is recorded in a {@code retry_tasks} collection and
 * retried by {@link RetryRelay} with backoff, moving to a DLQ after too many attempts - see the ADR
 * 0004 M10 addendum (flight-service) for why this is Mongo-backed rather than a literal second
 * Kafka topic.
 */
@ApplicationScoped
public class BookingConfirmedConsumer {

    private static final Logger LOG = Logger.getLogger(BookingConfirmedConsumer.class);

    private final ObjectMapper objectMapper;
    private final SendBookingConfirmationUseCase sendBookingConfirmationUseCase;
    private final MongoCollection<Document> processedBookings;
    private final MongoCollection<Document> retryTasks;

    public BookingConfirmedConsumer(
            ObjectMapper objectMapper,
            SendBookingConfirmationUseCase sendBookingConfirmationUseCase,
            MongoClient mongoClient,
            @ConfigProperty(name = "notification.mongo.database", defaultValue = "notification")
                    String database) {
        this.objectMapper = objectMapper;
        this.sendBookingConfirmationUseCase = sendBookingConfirmationUseCase;
        var db = mongoClient.getDatabase(database);
        this.processedBookings = db.getCollection("processed_bookings");
        this.retryTasks = db.getCollection("retry_tasks");
    }

    @Incoming("booking-confirmed")
    public void onMessage(String payload) {
        BookingConfirmedEvent event;
        try {
            event = objectMapper.readValue(payload, BookingConfirmedEvent.class);
        } catch (Exception e) {
            LOG.error("Malformed booking-confirmed payload, dropping: " + payload, e);
            return;
        }

        process(event.bookingId().value(), event.travelerEmail().value());
    }

    private void process(String bookingId, String recipientEmail) {
        if (!claim(bookingId)) {
            return;
        }

        try {
            sendBookingConfirmationUseCase.send(
                    new SendBookingConfirmationCommand(bookingId, recipientEmail));
        } catch (RuntimeException e) {
            LOG.error("Failed to send booking confirmation for booking " + bookingId, e);
            scheduleRetry(bookingId, recipientEmail, e.getMessage());
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

    private void scheduleRetry(String bookingId, String recipientEmail, String reason) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("bookingId", bookingId)
                        .append("recipientEmail", recipientEmail)
                        .append("attempts", 0)
                        .append("lastError", reason)
                        .append("nextAttemptAt", Date.from(Instant.now())));
    }
}

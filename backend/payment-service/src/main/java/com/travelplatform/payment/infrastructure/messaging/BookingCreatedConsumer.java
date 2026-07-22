package com.travelplatform.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.travelplatform.payment.application.port.in.AuthorizePaymentUseCase;
import com.travelplatform.payment.application.port.in.AuthorizePaymentUseCase.AuthorizePaymentCommand;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code booking-created} (own consumer group "payment-processor" - see application.yml)
 * and authorizes payment for the booking - the first leg of the choreography saga documented in
 * docs/adr/0010-payment-saga.md.
 *
 * <p>Idempotency: a {@code processed_bookings} collection, keyed by bookingId, is claimed before
 * calling {@link AuthorizePaymentUseCase} - Kafka's at-least-once delivery means the same message
 * can arrive twice, and re-authorizing an already-processed booking would double-charge.
 *
 * <p>Failure handling: a poison (unparseable) message is logged and dropped - retrying it can never
 * succeed. A technical failure (Mongo error) does not nack the message (Kafka has no delayed
 * redelivery); instead it is recorded in a {@code retry_tasks} collection and retried by {@link
 * RetryRelay} with backoff, moving to a DLQ after too many attempts - see the ADR 0004 M10 addendum
 * (flight-service) for why this is Mongo-backed rather than a literal second Kafka topic. A
 * *declined* authorization is not a failure from this consumer's point of view - it is successfully
 * recorded as {@code Payment.fail}, which publishes payment-failed for booking-service to react to.
 */
@ApplicationScoped
public class BookingCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(BookingCreatedConsumer.class);

    private final ObjectMapper objectMapper;
    private final AuthorizePaymentUseCase authorizePaymentUseCase;
    private final MongoCollection<Document> processedBookings;
    private final MongoCollection<Document> retryTasks;

    public BookingCreatedConsumer(
            ObjectMapper objectMapper,
            AuthorizePaymentUseCase authorizePaymentUseCase,
            MongoClient mongoClient,
            @ConfigProperty(name = "payment.mongo.database", defaultValue = "payment")
                    String database) {
        this.objectMapper = objectMapper;
        this.authorizePaymentUseCase = authorizePaymentUseCase;
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

        process(event.bookingId().value(), event.amount().amount(), event.amount().currency());
    }

    private void process(String bookingId, BigDecimal amount, String currency) {
        if (!claim(bookingId)) {
            return;
        }

        try {
            authorizePaymentUseCase.authorize(
                    new AuthorizePaymentCommand(bookingId, amount, currency));
        } catch (RuntimeException e) {
            LOG.error("Failed to authorize payment for booking " + bookingId, e);
            RetryTasks.schedule(
                    retryTasks, "booking-created", bookingId, amount, currency, e.getMessage());
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
}

package com.travelplatform.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.travelplatform.payment.application.port.in.RefundPaymentUseCase;
import com.travelplatform.payment.application.port.in.RefundPaymentUseCase.RefundPaymentCommand;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code booking-cancelled} (own consumer group "payment-processor") and refunds the
 * booking's payment if one was authorized - the compensating leg of the saga (ADR 0010).
 *
 * <p>A {@link com.travelplatform.payment.domain.payment.PaymentNotFoundException} (booking
 * cancelled before this service's booking-created consumer got to it) is not treated specially - it
 * is a RuntimeException like any other technical failure, so it goes through the same retry path
 * and resolves itself once the payment record exists.
 */
@ApplicationScoped
public class BookingCancelledConsumer {

    private static final Logger LOG = Logger.getLogger(BookingCancelledConsumer.class);

    private final ObjectMapper objectMapper;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final MongoCollection<Document> processedCancellations;
    private final MongoCollection<Document> retryTasks;

    public BookingCancelledConsumer(
            ObjectMapper objectMapper,
            RefundPaymentUseCase refundPaymentUseCase,
            MongoClient mongoClient,
            @ConfigProperty(name = "payment.mongo.database", defaultValue = "payment")
                    String database) {
        this.objectMapper = objectMapper;
        this.refundPaymentUseCase = refundPaymentUseCase;
        var db = mongoClient.getDatabase(database);
        this.processedCancellations = db.getCollection("processed_cancellations");
        this.retryTasks = db.getCollection("retry_tasks");
    }

    @Incoming("booking-cancelled")
    public void onMessage(String payload) {
        BookingCancelledEvent event;
        try {
            event = objectMapper.readValue(payload, BookingCancelledEvent.class);
        } catch (Exception e) {
            LOG.error("Malformed booking-cancelled payload, dropping: " + payload, e);
            return;
        }

        process(event.bookingId().value());
    }

    private void process(String bookingId) {
        if (!claim(bookingId)) {
            return;
        }

        try {
            refundPaymentUseCase.refund(new RefundPaymentCommand(bookingId));
        } catch (RuntimeException e) {
            LOG.error("Failed to refund payment for booking " + bookingId, e);
            RetryTasks.schedule(
                    retryTasks, "booking-cancelled", bookingId, null, null, e.getMessage());
        }
    }

    private boolean claim(String bookingId) {
        try {
            processedCancellations.insertOne(
                    new Document("_id", bookingId).append("processedAt", Date.from(Instant.now())));
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }
}

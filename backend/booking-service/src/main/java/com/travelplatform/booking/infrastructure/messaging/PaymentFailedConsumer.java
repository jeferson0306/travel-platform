package com.travelplatform.booking.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.travelplatform.booking.application.port.in.CancelBookingUseCase;
import com.travelplatform.booking.application.port.in.CancelBookingUseCase.CancelBookingCommand;
import com.travelplatform.booking.domain.booking.BookingAlreadyCancelledException;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code payment-failed} (own consumer group "booking-payment-outcome"), the compensating
 * leg of the saga (ADR 0010): payment-service declined the charge, so the booking is cancelled -
 * reusing the exact same {@link CancelBookingUseCase} the public cancel endpoint uses.
 *
 * <p>Idempotency: {@code Booking.cancel()} itself throws {@link BookingAlreadyCancelledException}
 * on a booking that is already cancelled - a duplicate delivery, or a booking the traveler already
 * cancelled manually, is naturally a no-op. See {@link PaymentAuthorizedConsumer} for the rest of
 * the failure-handling rationale.
 */
@ApplicationScoped
public class PaymentFailedConsumer {

    private static final Logger LOG = Logger.getLogger(PaymentFailedConsumer.class);

    private final ObjectMapper objectMapper;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final MongoCollection<Document> retryTasks;

    public PaymentFailedConsumer(
            ObjectMapper objectMapper,
            CancelBookingUseCase cancelBookingUseCase,
            MongoClient mongoClient,
            @ConfigProperty(name = "booking.mongo.database", defaultValue = "booking")
                    String database) {
        this.objectMapper = objectMapper;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.retryTasks = mongoClient.getDatabase(database).getCollection("retry_tasks");
    }

    @Incoming("payment-failed")
    public void onMessage(String payload) {
        PaymentOutcomeEvent event;
        try {
            event = objectMapper.readValue(payload, PaymentOutcomeEvent.class);
        } catch (Exception e) {
            LOG.error("Malformed payment-failed payload, dropping: " + payload, e);
            return;
        }

        var bookingId = event.bookingId().value();
        try {
            cancelBookingUseCase.cancel(new CancelBookingCommand(bookingId));
        } catch (BookingAlreadyCancelledException e) {
            LOG.info("Booking " + bookingId + " already cancelled, ignoring duplicate delivery");
        } catch (RuntimeException e) {
            LOG.error("Failed to cancel booking " + bookingId, e);
            RetryTasks.schedule(retryTasks, "payment-failed", bookingId, e.getMessage());
        }
    }
}

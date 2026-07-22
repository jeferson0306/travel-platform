package com.travelplatform.booking.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.travelplatform.booking.application.port.in.ConfirmBookingUseCase;
import com.travelplatform.booking.application.port.in.ConfirmBookingUseCase.ConfirmBookingCommand;
import com.travelplatform.booking.domain.booking.BookingAlreadyConfirmedException;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes {@code payment-authorized} (own consumer group "booking-payment-outcome" - see
 * application.yml), the second leg of the choreography saga (ADR 0010): payment-service approved
 * the charge, so the booking is confirmed.
 *
 * <p>Idempotency: {@code Booking.confirm()} itself throws {@link BookingAlreadyConfirmedException}
 * on a booking that is already confirmed - a duplicate delivery is naturally a no-op with no
 * separate claim collection needed, unlike the M10 inventory consumers (which mutate a counter that
 * has no such built-in guard). A technical failure (Mongo error, unexpectedly missing booking) is
 * recorded in {@code retry_tasks} and retried by {@link RetryRelay} - see the ADR 0004 M10 addendum
 * (flight-service) for why this is Mongo-backed rather than a literal second Kafka topic.
 */
@ApplicationScoped
public class PaymentAuthorizedConsumer {

    private static final Logger LOG = Logger.getLogger(PaymentAuthorizedConsumer.class);

    private final ObjectMapper objectMapper;
    private final ConfirmBookingUseCase confirmBookingUseCase;
    private final MongoCollection<Document> retryTasks;

    public PaymentAuthorizedConsumer(
            ObjectMapper objectMapper,
            ConfirmBookingUseCase confirmBookingUseCase,
            MongoClient mongoClient,
            @ConfigProperty(name = "booking.mongo.database", defaultValue = "booking")
                    String database) {
        this.objectMapper = objectMapper;
        this.confirmBookingUseCase = confirmBookingUseCase;
        this.retryTasks = mongoClient.getDatabase(database).getCollection("retry_tasks");
    }

    @Incoming("payment-authorized")
    public void onMessage(String payload) {
        PaymentOutcomeEvent event;
        try {
            event = objectMapper.readValue(payload, PaymentOutcomeEvent.class);
        } catch (Exception e) {
            LOG.error("Malformed payment-authorized payload, dropping: " + payload, e);
            return;
        }

        var bookingId = event.bookingId().value();
        try {
            confirmBookingUseCase.confirm(new ConfirmBookingCommand(bookingId));
        } catch (BookingAlreadyConfirmedException e) {
            LOG.info("Booking " + bookingId + " already confirmed, ignoring duplicate delivery");
        } catch (RuntimeException e) {
            LOG.error("Failed to confirm booking " + bookingId, e);
            RetryTasks.schedule(retryTasks, "payment-authorized", bookingId, e.getMessage());
        }
    }
}

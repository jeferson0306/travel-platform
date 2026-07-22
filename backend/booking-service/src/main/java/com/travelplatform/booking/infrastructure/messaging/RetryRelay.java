package com.travelplatform.booking.infrastructure.messaging;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.travelplatform.booking.application.port.in.CancelBookingUseCase;
import com.travelplatform.booking.application.port.in.CancelBookingUseCase.CancelBookingCommand;
import com.travelplatform.booking.application.port.in.ConfirmBookingUseCase;
import com.travelplatform.booking.application.port.in.ConfirmBookingUseCase.ConfirmBookingCommand;
import com.travelplatform.booking.domain.booking.BookingAlreadyCancelledException;
import com.travelplatform.booking.domain.booking.BookingAlreadyConfirmedException;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

/**
 * Retries {@code retry_tasks} left behind by {@link PaymentAuthorizedConsumer}/{@link
 * PaymentFailedConsumer} with exponential backoff, dispatching on the task's {@code action} field.
 * After {@link #MAX_ATTEMPTS} failed attempts, the task is moved to a {@code dead_letters}
 * collection and a summary is published to the DLQ topic matching its originating topic/consumer
 * group - see docs/adr/0004-use-kafka-for-event-driven-communication.md's M10 addendum.
 */
@ApplicationScoped
public class RetryRelay {

    private static final Logger LOG = Logger.getLogger(RetryRelay.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_BACKOFF_SECONDS = 10;

    private final MongoCollection<Document> retryTasks;
    private final MongoCollection<Document> deadLetters;
    private final ConfirmBookingUseCase confirmBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final Emitter<String> paymentAuthorizedDlqEmitter;
    private final Emitter<String> paymentFailedDlqEmitter;

    public RetryRelay(
            MongoClient mongoClient,
            @ConfigProperty(name = "booking.mongo.database", defaultValue = "booking")
                    String database,
            ConfirmBookingUseCase confirmBookingUseCase,
            CancelBookingUseCase cancelBookingUseCase,
            @Channel("payment-authorized-dlq") Emitter<String> paymentAuthorizedDlqEmitter,
            @Channel("payment-failed-dlq") Emitter<String> paymentFailedDlqEmitter) {
        var db = mongoClient.getDatabase(database);
        this.retryTasks = db.getCollection("retry_tasks");
        this.deadLetters = db.getCollection("dead_letters");
        this.confirmBookingUseCase = confirmBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.paymentAuthorizedDlqEmitter = paymentAuthorizedDlqEmitter;
        this.paymentFailedDlqEmitter = paymentFailedDlqEmitter;
    }

    @Scheduled(every = "10s")
    void relay() {
        for (Document task :
                retryTasks.find(Filters.lte("nextAttemptAt", Date.from(Instant.now())))) {
            attempt(task);
        }
    }

    private void attempt(Document task) {
        var id = task.getString("_id");
        var action = task.getString("action");
        var bookingId = task.getString("bookingId");
        var attempts = task.getInteger("attempts") + 1;

        boolean succeeded;
        try {
            dispatch(action, bookingId);
            succeeded = true;
        } catch (RuntimeException e) {
            LOG.error("Retry attempt " + attempts + " failed for booking " + bookingId, e);
            succeeded = false;
        }

        if (succeeded) {
            retryTasks.deleteOne(Filters.eq("_id", id));
            return;
        }

        if (attempts >= MAX_ATTEMPTS) {
            deadLetter(task, attempts, action);
            retryTasks.deleteOne(Filters.eq("_id", id));
            return;
        }

        var backoffSeconds = BASE_BACKOFF_SECONDS * (1L << attempts);
        retryTasks.updateOne(
                Filters.eq("_id", id),
                Updates.combine(
                        Updates.set("attempts", attempts),
                        Updates.set(
                                "nextAttemptAt",
                                Date.from(Instant.now().plusSeconds(backoffSeconds)))));
    }

    private void dispatch(String action, String bookingId) {
        if ("payment-authorized".equals(action)) {
            try {
                confirmBookingUseCase.confirm(new ConfirmBookingCommand(bookingId));
            } catch (BookingAlreadyConfirmedException e) {
                // Resolved since this task was scheduled (e.g. a prior attempt succeeded but the
                // task delete raced with a new attempt) - treat as done, not a failure.
            }
        } else {
            try {
                cancelBookingUseCase.cancel(new CancelBookingCommand(bookingId));
            } catch (BookingAlreadyCancelledException e) {
                // See above.
            }
        }
    }

    private void deadLetter(Document task, int attempts, String action) {
        var bookingId = task.getString("bookingId");
        var deadLetterDocument =
                new Document(task)
                        .append("attempts", attempts)
                        .append("deadLetteredAt", Date.from(Instant.now()));
        // Upsert, not insert: the real @Scheduled trigger and a manual/direct relay() call could
        // race on the same overdue task (same source _id) - an upsert makes that harmless instead
        // of a duplicate-key crash.
        deadLetters.replaceOne(
                Filters.eq("_id", deadLetterDocument.getString("_id")),
                deadLetterDocument,
                new ReplaceOptions().upsert(true));
        LOG.error(
                "Booking "
                        + bookingId
                        + " ("
                        + action
                        + ") moved to DLQ after "
                        + attempts
                        + " attempts");

        var emitter =
                "payment-authorized".equals(action)
                        ? paymentAuthorizedDlqEmitter
                        : paymentFailedDlqEmitter;
        emitter.send(
                "{\"bookingId\":\""
                        + bookingId
                        + "\",\"action\":\""
                        + action
                        + "\",\"attempts\":"
                        + attempts
                        + "}");
    }
}

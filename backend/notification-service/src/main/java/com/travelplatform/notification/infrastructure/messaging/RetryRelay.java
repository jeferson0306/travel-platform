package com.travelplatform.notification.infrastructure.messaging;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.travelplatform.notification.application.port.in.SendBookingConfirmationUseCase;
import com.travelplatform.notification.application.port.in.SendBookingConfirmationUseCase.SendBookingConfirmationCommand;
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
 * Retries {@code retry_tasks} left behind by {@link BookingConfirmedConsumer} with exponential
 * backoff. After {@link #MAX_ATTEMPTS} failed attempts, the task is moved to a {@code dead_letters}
 * collection and a summary is published to the {@code booking-confirmed-dlq} Kafka topic - see
 * docs/adr/0004-use-kafka-for-event-driven-communication.md's M10 addendum. Only one kind of retry
 * task exists here (unlike booking/payment-service's RetryRelay), so there is no action dispatch.
 */
@ApplicationScoped
public class RetryRelay {

    private static final Logger LOG = Logger.getLogger(RetryRelay.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_BACKOFF_SECONDS = 10;

    private final MongoCollection<Document> retryTasks;
    private final MongoCollection<Document> deadLetters;
    private final SendBookingConfirmationUseCase sendBookingConfirmationUseCase;
    private final Emitter<String> dlqEmitter;

    public RetryRelay(
            MongoClient mongoClient,
            @ConfigProperty(name = "notification.mongo.database", defaultValue = "notification")
                    String database,
            SendBookingConfirmationUseCase sendBookingConfirmationUseCase,
            @Channel("booking-confirmed-dlq") Emitter<String> dlqEmitter) {
        var db = mongoClient.getDatabase(database);
        this.retryTasks = db.getCollection("retry_tasks");
        this.deadLetters = db.getCollection("dead_letters");
        this.sendBookingConfirmationUseCase = sendBookingConfirmationUseCase;
        this.dlqEmitter = dlqEmitter;
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
        var bookingId = task.getString("bookingId");
        var recipientEmail = task.getString("recipientEmail");
        var attempts = task.getInteger("attempts") + 1;

        boolean succeeded;
        try {
            sendBookingConfirmationUseCase.send(
                    new SendBookingConfirmationCommand(bookingId, recipientEmail));
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
            deadLetter(task, attempts);
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

    private void deadLetter(Document task, int attempts) {
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
        LOG.error("Booking " + bookingId + " moved to DLQ after " + attempts + " attempts");
        dlqEmitter.send("{\"bookingId\":\"" + bookingId + "\",\"attempts\":" + attempts + "}");
    }
}

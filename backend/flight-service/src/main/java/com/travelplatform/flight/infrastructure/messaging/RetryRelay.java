package com.travelplatform.flight.infrastructure.messaging;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.travelplatform.flight.application.port.in.ReserveInventoryUseCase;
import com.travelplatform.flight.application.port.in.ReserveInventoryUseCase.ReserveInventoryCommand;
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
 * Retries {@code retry_tasks} left behind by {@link BookingCreatedConsumer} with exponential
 * backoff. After {@link #MAX_ATTEMPTS} failed attempts, the task is moved to a {@code dead_letters}
 * collection and a summary is published to the {@code booking-created-dlq} Kafka topic so it is
 * externally visible/alertable, not just sitting in Mongo - see ADR 0004.
 */
@ApplicationScoped
public class RetryRelay {

    private static final Logger LOG = Logger.getLogger(RetryRelay.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long BASE_BACKOFF_SECONDS = 10;

    private final MongoCollection<Document> retryTasks;
    private final MongoCollection<Document> deadLetters;
    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final Emitter<String> dlqEmitter;

    public RetryRelay(
            MongoClient mongoClient,
            @ConfigProperty(name = "flight.mongo.database", defaultValue = "flight")
                    String database,
            ReserveInventoryUseCase reserveInventoryUseCase,
            @Channel("booking-created-dlq") Emitter<String> dlqEmitter) {
        var db = mongoClient.getDatabase(database);
        this.retryTasks = db.getCollection("retry_tasks");
        this.deadLetters = db.getCollection("dead_letters");
        this.reserveInventoryUseCase = reserveInventoryUseCase;
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
        var itemId = task.getString("itemId");
        var quantity = task.getInteger("quantity");
        var attempts = task.getInteger("attempts") + 1;

        boolean reserved;
        try {
            reserved =
                    reserveInventoryUseCase.reserve(new ReserveInventoryCommand(itemId, quantity));
        } catch (RuntimeException e) {
            LOG.error("Retry attempt " + attempts + " failed for booking " + bookingId, e);
            reserved = false;
        }

        if (reserved) {
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
        dlqEmitter.send(
                "{\"bookingId\":\""
                        + bookingId
                        + "\",\"itemId\":\""
                        + task.getString("itemId")
                        + "\",\"attempts\":"
                        + attempts
                        + "}");
    }
}

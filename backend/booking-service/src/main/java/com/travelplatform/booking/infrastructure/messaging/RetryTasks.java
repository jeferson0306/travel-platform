package com.travelplatform.booking.infrastructure.messaging;

import com.mongodb.client.MongoCollection;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;

/**
 * Builds a {@code retry_tasks} document shared by {@link PaymentAuthorizedConsumer} and {@link
 * PaymentFailedConsumer} - {@link RetryRelay} dispatches on the {@code action} field.
 */
final class RetryTasks {

    private RetryTasks() {}

    static void schedule(
            MongoCollection<Document> retryTasks, String action, String bookingId, String reason) {
        retryTasks.insertOne(
                new Document("_id", UUID.randomUUID().toString())
                        .append("action", action)
                        .append("bookingId", bookingId)
                        .append("attempts", 0)
                        .append("lastError", reason)
                        .append("nextAttemptAt", Date.from(Instant.now())));
    }
}

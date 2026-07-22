package com.travelplatform.payment.infrastructure.messaging;

import com.mongodb.client.MongoCollection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;

/**
 * Builds a {@code retry_tasks} document shared by {@link BookingCreatedConsumer} and {@link
 * BookingCancelledConsumer} - {@link RetryRelay} dispatches on the {@code action} field. {@code
 * amountValue}/{@code amountCurrency} are only present for a "booking-created" task (a
 * "booking-cancelled" retry re-derives the amount from the stored Payment, not from this task).
 */
final class RetryTasks {

    private RetryTasks() {}

    static void schedule(
            MongoCollection<Document> retryTasks,
            String action,
            String bookingId,
            BigDecimal amount,
            String currency,
            String reason) {
        var document =
                new Document("_id", UUID.randomUUID().toString())
                        .append("action", action)
                        .append("bookingId", bookingId)
                        .append("attempts", 0)
                        .append("lastError", reason)
                        .append("nextAttemptAt", Date.from(Instant.now()));
        if (amount != null) {
            document.append("amountValue", amount.toPlainString())
                    .append("amountCurrency", currency);
        }
        retryTasks.insertOne(document);
    }
}

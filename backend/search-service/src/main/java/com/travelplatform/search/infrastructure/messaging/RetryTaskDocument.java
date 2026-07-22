package com.travelplatform.search.infrastructure.messaging;

import java.time.Instant;

/**
 * Wire shape stored in the "retry_tasks"/"dead_letters" OpenSearch indices - the OpenSearch
 * counterpart to the Mongo-backed {@code retry_tasks}/{@code dead_letters} collections used by
 * every other service (see docs/adr/0012-search-service-opensearch.md). {@code eventType}
 * ("flight-created" or "hotel-created") plays the same dispatch role payment-service's {@code
 * action} field plays in its RetryRelay.
 */
public class RetryTaskDocument {

    public String eventType;
    public String payload;
    public int attempts;
    public String lastError;
    public Instant nextAttemptAt;

    public RetryTaskDocument() {}

    public RetryTaskDocument(
            String eventType,
            String payload,
            int attempts,
            String lastError,
            Instant nextAttemptAt) {
        this.eventType = eventType;
        this.payload = payload;
        this.attempts = attempts;
        this.lastError = lastError;
        this.nextAttemptAt = nextAttemptAt;
    }
}

package com.travelplatform.booking.infrastructure.persistence.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.booking.domain.shared.DomainEvent;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;

/** Converts a {@link DomainEvent} into its transactional-outbox MongoDB representation. */
public final class OutboxDocumentMapper {

    private OutboxDocumentMapper() {}

    public static Document toDocument(DomainEvent event, ObjectMapper objectMapper) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize domain event " + event.eventType(), e);
        }

        return new Document("_id", UUID.randomUUID().toString())
                .append("eventType", event.eventType())
                .append("payload", payload)
                .append("createdAt", Date.from(event.occurredOn()))
                .append("publishedAt", null);
    }
}

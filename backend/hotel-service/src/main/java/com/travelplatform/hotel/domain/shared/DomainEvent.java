package com.travelplatform.hotel.domain.shared;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredOn();

    /** Used as the outbox document's discriminator and Kafka message key/type. */
    String eventType();
}

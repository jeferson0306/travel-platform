package com.travelplatform.identity.domain.shared;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredOn();
}

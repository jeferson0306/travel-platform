package com.travelplatform.identity.domain.user;

import com.travelplatform.identity.domain.shared.DomainEvent;
import java.time.Instant;

public record UserRegistered(UserId userId, Email email, Instant occurredOn)
        implements DomainEvent {}

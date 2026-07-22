package com.travelplatform.identity.domain.user;

import com.travelplatform.identity.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Aggregate root for the identity bounded context. */
public final class User {

    private final UserId id;
    private final Email email;
    private final HashedPassword password;
    private final UserStatus status;
    private final Instant registeredAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private User(
            UserId id,
            Email email,
            HashedPassword password,
            UserStatus status,
            Instant registeredAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.status = status;
        this.registeredAt = registeredAt;
    }

    /** Registers a new user, recording a {@link UserRegistered} domain event. */
    public static User register(Email email, HashedPassword password) {
        var registeredAt = Instant.now();
        var user = new User(UserId.newId(), email, password, UserStatus.ACTIVE, registeredAt);
        user.domainEvents.add(new UserRegistered(user.id, user.email, registeredAt));
        return user;
    }

    /** Rebuilds a user from persisted state. Does not raise domain events. */
    public static User reconstitute(
            UserId id,
            Email email,
            HashedPassword password,
            UserStatus status,
            Instant registeredAt) {
        return new User(id, email, password, status, registeredAt);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public HashedPassword password() {
        return password;
    }

    public UserStatus status() {
        return status;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    /** Returns and clears the domain events raised since this instance was created. */
    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}

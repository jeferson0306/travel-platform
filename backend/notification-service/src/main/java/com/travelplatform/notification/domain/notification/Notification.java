package com.travelplatform.notification.domain.notification;

import java.time.Instant;

/**
 * Aggregate root for the notification bounded context. Unlike every other aggregate in this
 * platform, this one raises no domain events and needs no transactional outbox - it is a terminal
 * consumer (nothing downstream reacts to "a notification was sent"), so there is nothing to
 * atomically publish alongside the write. See docs/adr/0011-notification-service.md.
 */
public final class Notification {

    private final NotificationId id;
    private final BookingId bookingId;
    private final Email recipient;
    private final NotificationType type;
    private final NotificationStatus status;
    private final Instant createdAt;

    private Notification(
            NotificationId id,
            BookingId bookingId,
            Email recipient,
            NotificationType type,
            NotificationStatus status,
            Instant createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.recipient = recipient;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Notification sent(BookingId bookingId, Email recipient, NotificationType type) {
        return new Notification(
                NotificationId.newId(),
                bookingId,
                recipient,
                type,
                NotificationStatus.SENT,
                Instant.now());
    }

    public static Notification failed(BookingId bookingId, Email recipient, NotificationType type) {
        return new Notification(
                NotificationId.newId(),
                bookingId,
                recipient,
                type,
                NotificationStatus.FAILED,
                Instant.now());
    }

    /** Rebuilds a notification from persisted state. */
    public static Notification reconstitute(
            NotificationId id,
            BookingId bookingId,
            Email recipient,
            NotificationType type,
            NotificationStatus status,
            Instant createdAt) {
        return new Notification(id, bookingId, recipient, type, status, createdAt);
    }

    public NotificationId id() {
        return id;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public Email recipient() {
        return recipient;
    }

    public NotificationType type() {
        return type;
    }

    public NotificationStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

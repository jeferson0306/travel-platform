package com.travelplatform.notification.domain.notification;

import java.util.Objects;
import java.util.UUID;

public record NotificationId(UUID value) {

    public NotificationId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static NotificationId newId() {
        return new NotificationId(UUID.randomUUID());
    }

    public static NotificationId of(String raw) {
        return new NotificationId(UUID.fromString(raw));
    }
}

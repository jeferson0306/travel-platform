package com.travelplatform.notification.api.dto;

import com.travelplatform.notification.domain.notification.Notification;
import java.time.Instant;

public record NotificationResponse(
        String notificationId,
        String bookingId,
        String recipient,
        String type,
        String status,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.id().value().toString(),
                notification.bookingId().value().toString(),
                notification.recipient().value(),
                notification.type().name(),
                notification.status().name(),
                notification.createdAt());
    }
}

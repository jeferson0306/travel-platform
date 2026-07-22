package com.travelplatform.notification.infrastructure.persistence.mongo;

import com.travelplatform.notification.domain.notification.BookingId;
import com.travelplatform.notification.domain.notification.Email;
import com.travelplatform.notification.domain.notification.Notification;
import com.travelplatform.notification.domain.notification.NotificationId;
import com.travelplatform.notification.domain.notification.NotificationStatus;
import com.travelplatform.notification.domain.notification.NotificationType;
import java.util.Date;
import org.bson.Document;

public final class NotificationDocumentMapper {

    private NotificationDocumentMapper() {}

    public static Document toDocument(Notification notification) {
        return new Document("_id", notification.id().value().toString())
                .append("bookingId", notification.bookingId().value().toString())
                .append("recipient", notification.recipient().value())
                .append("type", notification.type().name())
                .append("status", notification.status().name())
                .append("createdAt", Date.from(notification.createdAt()));
    }

    public static Notification toDomain(Document document) {
        return Notification.reconstitute(
                NotificationId.of(document.getString("_id")),
                BookingId.of(document.getString("bookingId")),
                new Email(document.getString("recipient")),
                NotificationType.valueOf(document.getString("type")),
                NotificationStatus.valueOf(document.getString("status")),
                document.getDate("createdAt").toInstant());
    }
}

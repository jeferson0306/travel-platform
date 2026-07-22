package com.travelplatform.notification.infrastructure.persistence.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.travelplatform.notification.application.port.out.NotificationRepository;
import com.travelplatform.notification.domain.notification.BookingId;
import com.travelplatform.notification.domain.notification.Notification;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** No transactional outbox here - see {@link Notification}'s javadoc for why. */
@ApplicationScoped
public class MongoNotificationRepository implements NotificationRepository {

    private final MongoCollection<Document> notifications;

    public MongoNotificationRepository(
            MongoClient mongoClient,
            @ConfigProperty(name = "notification.mongo.database", defaultValue = "notification")
                    String database) {
        this.notifications = mongoClient.getDatabase(database).getCollection("notifications");
    }

    @Override
    public void save(Notification notification) {
        var document = NotificationDocumentMapper.toDocument(notification);
        notifications.replaceOne(
                Filters.eq("_id", document.getString("_id")),
                document,
                new ReplaceOptions().upsert(true));
    }

    @Override
    public Optional<Notification> findByBookingId(BookingId bookingId) {
        return Optional.ofNullable(
                        notifications
                                .find(Filters.eq("bookingId", bookingId.value().toString()))
                                .first())
                .map(NotificationDocumentMapper::toDomain);
    }
}

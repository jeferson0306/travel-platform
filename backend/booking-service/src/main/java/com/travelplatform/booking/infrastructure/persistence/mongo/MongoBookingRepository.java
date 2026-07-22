package com.travelplatform.booking.infrastructure.persistence.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Persists a booking and its pulled domain events to the transactional outbox in one MongoDB
 * transaction - see docs/adr/0007-transactional-outbox.md.
 */
@ApplicationScoped
public class MongoBookingRepository implements BookingRepository {

    private final MongoClient mongoClient;
    private final MongoCollection<Document> bookings;
    private final MongoCollection<Document> outbox;
    private final ObjectMapper objectMapper;

    public MongoBookingRepository(
            MongoClient mongoClient,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "booking.mongo.database", defaultValue = "booking")
                    String database) {
        this.mongoClient = mongoClient;
        this.objectMapper = objectMapper;
        var db = mongoClient.getDatabase(database);
        this.bookings = db.getCollection("bookings");
        this.outbox = db.getCollection("outbox");
    }

    @Override
    public void save(Booking booking) {
        var events = booking.pullDomainEvents();
        var document = BookingDocumentMapper.toDocument(booking);

        try (ClientSession session = mongoClient.startSession()) {
            session.<Void>withTransaction(
                    () -> {
                        bookings.replaceOne(
                                session,
                                Filters.eq("_id", document.getString("_id")),
                                document,
                                new ReplaceOptions().upsert(true));
                        events.forEach(
                                event ->
                                        outbox.insertOne(
                                                session,
                                                OutboxDocumentMapper.toDocument(
                                                        event, objectMapper)));
                        return null;
                    });
        }
    }

    @Override
    public Optional<Booking> findById(BookingId id) {
        return Optional.ofNullable(bookings.find(Filters.eq("_id", id.value().toString())).first())
                .map(BookingDocumentMapper::toDomain);
    }
}

package com.travelplatform.payment.infrastructure.persistence.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.travelplatform.payment.application.port.out.PaymentRepository;
import com.travelplatform.payment.domain.payment.BookingId;
import com.travelplatform.payment.domain.payment.Payment;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Persists a payment and its pulled domain events to the transactional outbox in one MongoDB
 * transaction - see docs/adr/0007-transactional-outbox.md (booking-service).
 */
@ApplicationScoped
public class MongoPaymentRepository implements PaymentRepository {

    private final MongoClient mongoClient;
    private final MongoCollection<Document> payments;
    private final MongoCollection<Document> outbox;
    private final ObjectMapper objectMapper;

    public MongoPaymentRepository(
            MongoClient mongoClient,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "payment.mongo.database", defaultValue = "payment")
                    String database) {
        this.mongoClient = mongoClient;
        this.objectMapper = objectMapper;
        var db = mongoClient.getDatabase(database);
        this.payments = db.getCollection("payments");
        this.outbox = db.getCollection("outbox");
    }

    @Override
    public void save(Payment payment) {
        var events = payment.pullDomainEvents();
        var document = PaymentDocumentMapper.toDocument(payment);

        try (ClientSession session = mongoClient.startSession()) {
            session.<Void>withTransaction(
                    () -> {
                        payments.replaceOne(
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
    public Optional<Payment> findByBookingId(BookingId bookingId) {
        return Optional.ofNullable(
                        payments.find(Filters.eq("bookingId", bookingId.value().toString()))
                                .first())
                .map(PaymentDocumentMapper::toDomain);
    }
}

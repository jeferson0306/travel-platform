package com.travelplatform.booking.infrastructure.messaging;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Polls the transactional outbox for unpublished events and sends each to its own Kafka topic (the
 * event's {@code eventType} - "booking-created", "booking-cancelled"), marking it published only
 * after a successful send. At-least-once delivery: a crash between send and mark-published
 * republishes on the next poll - see docs/adr/0007-transactional-outbox.md.
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class);

    private final MongoCollection<Document> outbox;
    private final Emitter<String> emitter;

    public OutboxRelay(
            MongoClient mongoClient,
            @ConfigProperty(name = "booking.mongo.database", defaultValue = "booking")
                    String database,
            @Channel("booking-events") Emitter<String> emitter) {
        this.outbox = mongoClient.getDatabase(database).getCollection("outbox");
        this.emitter = emitter;
    }

    @Scheduled(every = "2s")
    void relay() {
        for (Document event : outbox.find(Filters.eq("publishedAt", null))) {
            publish(event);
        }
    }

    private void publish(Document event) {
        var id = event.getString("_id");
        var eventType = event.getString("eventType");
        var payload = event.getString("payload");

        var metadata = OutgoingKafkaRecordMetadata.<String>builder().withTopic(eventType).build();
        var message =
                Message.of(payload)
                        .addMetadata(metadata)
                        .withAck(
                                () -> {
                                    markPublished(id);
                                    return CompletableFuture.completedFuture(null);
                                })
                        .withNack(
                                throwable -> {
                                    LOG.error("Failed to publish outbox event " + id, throwable);
                                    return CompletableFuture.completedFuture(null);
                                });

        emitter.send(message);
    }

    private void markPublished(String id) {
        outbox.updateOne(
                Filters.eq("_id", id), Updates.set("publishedAt", Date.from(Instant.now())));
    }
}

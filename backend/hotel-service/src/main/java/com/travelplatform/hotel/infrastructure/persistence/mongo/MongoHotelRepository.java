package com.travelplatform.hotel.infrastructure.persistence.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.travelplatform.hotel.application.port.out.HotelRepository;
import com.travelplatform.hotel.domain.hotel.City;
import com.travelplatform.hotel.domain.hotel.Hotel;
import com.travelplatform.hotel.domain.hotel.HotelId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Persists a hotel and its pulled domain events to the transactional outbox in one MongoDB
 * transaction - see docs/adr/0007-transactional-outbox.md.
 */
@ApplicationScoped
public class MongoHotelRepository implements HotelRepository {

    private final MongoClient mongoClient;
    private final MongoCollection<Document> hotels;
    private final MongoCollection<Document> outbox;
    private final ObjectMapper objectMapper;

    public MongoHotelRepository(
            MongoClient mongoClient,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "hotel.mongo.database", defaultValue = "hotel")
                    String database) {
        this.mongoClient = mongoClient;
        this.objectMapper = objectMapper;
        var db = mongoClient.getDatabase(database);
        this.hotels = db.getCollection("hotels");
        this.outbox = db.getCollection("outbox");
    }

    @Override
    public void save(Hotel hotel) {
        var events = hotel.pullDomainEvents();
        var document = HotelDocumentMapper.toDocument(hotel);

        try (ClientSession session = mongoClient.startSession()) {
            session.<Void>withTransaction(
                    () -> {
                        hotels.replaceOne(
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
    public Optional<Hotel> findById(HotelId id) {
        return Optional.ofNullable(hotels.find(Filters.eq("_id", id.value().toString())).first())
                .map(HotelDocumentMapper::toDomain);
    }

    @Override
    public List<Hotel> search(City city) {
        var results = new java.util.ArrayList<Hotel>();
        for (Document document : hotels.find(Filters.eq("city", city.value()))) {
            results.add(HotelDocumentMapper.toDomain(document));
        }
        return results;
    }

    @Override
    public boolean tryReserve(HotelId id, int quantity) {
        var filter =
                Filters.and(
                        Filters.eq("_id", id.value().toString()),
                        Filters.gte("availableRooms", quantity));
        var result = hotels.findOneAndUpdate(filter, Updates.inc("availableRooms", -quantity));
        return result != null;
    }
}

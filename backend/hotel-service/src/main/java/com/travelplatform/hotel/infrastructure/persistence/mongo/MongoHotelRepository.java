package com.travelplatform.hotel.infrastructure.persistence.mongo;

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

@ApplicationScoped
public class MongoHotelRepository implements HotelRepository {

    private final MongoCollection<Document> hotels;

    public MongoHotelRepository(
            MongoClient mongoClient,
            @ConfigProperty(name = "hotel.mongo.database", defaultValue = "hotel")
                    String database) {
        this.hotels = mongoClient.getDatabase(database).getCollection("hotels");
    }

    @Override
    public void save(Hotel hotel) {
        var document = HotelDocumentMapper.toDocument(hotel);
        hotels.replaceOne(
                Filters.eq("_id", document.getString("_id")),
                document,
                new ReplaceOptions().upsert(true));
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

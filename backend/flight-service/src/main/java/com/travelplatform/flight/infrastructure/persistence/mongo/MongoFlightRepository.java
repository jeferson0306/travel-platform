package com.travelplatform.flight.infrastructure.persistence.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.travelplatform.flight.application.port.out.FlightRepository;
import com.travelplatform.flight.domain.flight.AirportCode;
import com.travelplatform.flight.domain.flight.Flight;
import com.travelplatform.flight.domain.flight.FlightId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MongoFlightRepository implements FlightRepository {

    private final MongoCollection<Document> flights;

    public MongoFlightRepository(
            MongoClient mongoClient,
            @ConfigProperty(name = "flight.mongo.database", defaultValue = "flight")
                    String database) {
        this.flights = mongoClient.getDatabase(database).getCollection("flights");
    }

    @Override
    public void save(Flight flight) {
        var document = FlightDocumentMapper.toDocument(flight);
        flights.replaceOne(
                Filters.eq("_id", document.getString("_id")),
                document,
                new ReplaceOptions().upsert(true));
    }

    @Override
    public Optional<Flight> findById(FlightId id) {
        return Optional.ofNullable(flights.find(Filters.eq("_id", id.value().toString())).first())
                .map(FlightDocumentMapper::toDomain);
    }

    @Override
    public List<Flight> search(AirportCode origin, AirportCode destination) {
        var filter =
                Filters.and(
                        Filters.eq("origin", origin.value()),
                        Filters.eq("destination", destination.value()));
        var results = new java.util.ArrayList<Flight>();
        for (Document document : flights.find(filter)) {
            results.add(FlightDocumentMapper.toDomain(document));
        }
        return results;
    }
}

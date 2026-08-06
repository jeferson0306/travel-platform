package com.travelplatform.flight.infrastructure.persistence.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.travelplatform.flight.application.port.out.FlightRepository;
import com.travelplatform.flight.domain.flight.AirportCode;
import com.travelplatform.flight.domain.flight.Flight;
import com.travelplatform.flight.domain.flight.FlightId;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Persists a flight and its pulled domain events to the transactional outbox in one MongoDB
 * transaction - see docs/adr/0007-transactional-outbox.md.
 */
@ApplicationScoped
public class MongoFlightRepository implements FlightRepository {

    private final MongoClient mongoClient;
    private final MongoCollection<Document> flights;
    private final MongoCollection<Document> outbox;
    private final ObjectMapper objectMapper;

    public MongoFlightRepository(
            MongoClient mongoClient,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "flight.mongo.database", defaultValue = "flight")
                    String database) {
        this.mongoClient = mongoClient;
        this.objectMapper = objectMapper;
        var db = mongoClient.getDatabase(database);
        this.flights = db.getCollection("flights");
        this.outbox = db.getCollection("outbox");
    }

    @Override
    public void save(Flight flight) {
        var events = flight.pullDomainEvents();
        var document = FlightDocumentMapper.toDocument(flight);

        try (ClientSession session = mongoClient.startSession()) {
            session.<Void>withTransaction(
                    () -> {
                        flights.replaceOne(
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
    public Optional<Flight> findById(FlightId id) {
        return Optional.ofNullable(flights.find(Filters.eq("_id", id.value().toString())).first())
                .map(FlightDocumentMapper::toDomain);
    }

    @Override
    public List<Flight> search(
            AirportCode origin, AirportCode destination, LocalDate departureDate) {
        var conditions =
                new java.util.ArrayList<>(
                        List.of(
                                Filters.eq("origin", origin.value()),
                                Filters.eq("destination", destination.value())));
        if (departureDate != null) {
            // departureAt is stored as an instant with no traveler timezone attached anywhere in
            // this platform yet - treating the requested date as a UTC calendar day is the same
            // simplifying assumption the rest of the codebase makes for Instants.
            var startOfDay = Date.from(departureDate.atStartOfDay(ZoneOffset.UTC).toInstant());
            var startOfNextDay =
                    Date.from(departureDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
            conditions.add(Filters.gte("departureAt", startOfDay));
            conditions.add(Filters.lt("departureAt", startOfNextDay));
        }
        var filter = Filters.and(conditions);
        var results = new java.util.ArrayList<Flight>();
        for (Document document : flights.find(filter)) {
            results.add(FlightDocumentMapper.toDomain(document));
        }
        return results;
    }

    @Override
    public boolean tryReserve(FlightId id, int quantity) {
        var filter =
                Filters.and(
                        Filters.eq("_id", id.value().toString()),
                        Filters.gte("availableSeats", quantity));
        var result = flights.findOneAndUpdate(filter, Updates.inc("availableSeats", -quantity));
        return result != null;
    }
}

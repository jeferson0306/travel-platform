package com.travelplatform.flight.domain.flight;

import com.travelplatform.flight.domain.shared.DomainEvent;
import java.time.Instant;

/** Consumed by search-service to index this flight (ROADMAP M13). */
public record FlightCreated(
        FlightId flightId,
        AirportCode origin,
        AirportCode destination,
        Instant departureAt,
        Instant arrivalAt,
        Money price,
        int availableSeats,
        String airline,
        String airlineCode,
        String flightNumber,
        String cabinClass,
        int stops,
        Instant occurredOn)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "flight-created";
    }
}

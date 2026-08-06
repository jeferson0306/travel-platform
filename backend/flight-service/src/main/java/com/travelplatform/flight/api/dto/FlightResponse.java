package com.travelplatform.flight.api.dto;

import com.travelplatform.flight.domain.flight.Flight;
import java.math.BigDecimal;
import java.time.Instant;

public record FlightResponse(
        String id,
        String origin,
        String destination,
        Instant departureAt,
        Instant arrivalAt,
        BigDecimal priceAmount,
        String priceCurrency,
        int availableSeats,
        String airline,
        String airlineCode,
        String flightNumber,
        String cabinClass,
        int stops) {

    public static FlightResponse from(Flight flight) {
        return new FlightResponse(
                flight.id().value().toString(),
                flight.origin().value(),
                flight.destination().value(),
                flight.departureAt(),
                flight.arrivalAt(),
                flight.price().amount(),
                flight.price().currency(),
                flight.availableSeats(),
                flight.airline(),
                flight.airlineCode(),
                flight.flightNumber(),
                flight.cabinClass(),
                flight.stops());
    }
}

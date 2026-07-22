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
        int availableSeats) {

    public static FlightResponse from(Flight flight) {
        return new FlightResponse(
                flight.id().value().toString(),
                flight.origin().value(),
                flight.destination().value(),
                flight.departureAt(),
                flight.arrivalAt(),
                flight.price().amount(),
                flight.price().currency(),
                flight.availableSeats());
    }
}

package com.travelplatform.search.api.dto;

import com.travelplatform.search.domain.flight.SearchableFlight;
import java.math.BigDecimal;
import java.time.Instant;

public record FlightSearchResponse(
        String flightId,
        String origin,
        String destination,
        Instant departureAt,
        Instant arrivalAt,
        BigDecimal priceAmount,
        String priceCurrency,
        int availableSeats) {

    public static FlightSearchResponse from(SearchableFlight flight) {
        return new FlightSearchResponse(
                flight.flightId(),
                flight.origin(),
                flight.destination(),
                flight.departureAt(),
                flight.arrivalAt(),
                flight.priceAmount(),
                flight.priceCurrency(),
                flight.availableSeats());
    }
}

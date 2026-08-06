package com.travelplatform.search.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;

/** Driven by flight-service's flight-created event (ROADMAP M13), not a public endpoint. */
public interface IndexFlightUseCase {

    void index(IndexFlightCommand command);

    record IndexFlightCommand(
            String flightId,
            String origin,
            String destination,
            Instant departureAt,
            Instant arrivalAt,
            BigDecimal priceAmount,
            String priceCurrency,
            int availableSeats) {}
}

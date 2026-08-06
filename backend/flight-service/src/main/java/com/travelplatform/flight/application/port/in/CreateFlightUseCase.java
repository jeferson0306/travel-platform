package com.travelplatform.flight.application.port.in;

import com.travelplatform.flight.domain.flight.FlightId;
import java.math.BigDecimal;
import java.time.Instant;

public interface CreateFlightUseCase {

    FlightId create(CreateFlightCommand command);

    record CreateFlightCommand(
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
            int stops) {}
}

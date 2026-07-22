package com.travelplatform.search.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * The subset of flight-service's {@code FlightCreated} domain event this service cares about - see
 * docs/events/flight-events.md.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlightCreatedEvent(
        FlightIdDto flightId,
        AirportCodeDto origin,
        AirportCodeDto destination,
        Instant departureAt,
        Instant arrivalAt,
        MoneyDto price,
        int availableSeats) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlightIdDto(String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AirportCodeDto(String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MoneyDto(BigDecimal amount, String currency) {}
}

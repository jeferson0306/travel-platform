package com.travelplatform.flight.domain.flight;

import java.time.Instant;

/**
 * Aggregate root for the flight bounded context. No lifecycle yet - inventory is immutable once
 * created (no update/delete endpoint); revisit once a real need appears.
 */
public final class Flight {

    private final FlightId id;
    private final AirportCode origin;
    private final AirportCode destination;
    private final Instant departureAt;
    private final Instant arrivalAt;
    private final Money price;
    private final int availableSeats;

    private Flight(
            FlightId id,
            AirportCode origin,
            AirportCode destination,
            Instant departureAt,
            Instant arrivalAt,
            Money price,
            int availableSeats) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
        this.price = price;
        this.availableSeats = availableSeats;
    }

    public static Flight create(
            AirportCode origin,
            AirportCode destination,
            Instant departureAt,
            Instant arrivalAt,
            Money price,
            int availableSeats) {
        if (!arrivalAt.isAfter(departureAt)) {
            throw new InvalidFlightScheduleException();
        }
        if (availableSeats < 0) {
            throw new IllegalArgumentException("availableSeats must not be negative");
        }
        return new Flight(
                FlightId.newId(),
                origin,
                destination,
                departureAt,
                arrivalAt,
                price,
                availableSeats);
    }

    public static Flight reconstitute(
            FlightId id,
            AirportCode origin,
            AirportCode destination,
            Instant departureAt,
            Instant arrivalAt,
            Money price,
            int availableSeats) {
        return new Flight(id, origin, destination, departureAt, arrivalAt, price, availableSeats);
    }

    public FlightId id() {
        return id;
    }

    public AirportCode origin() {
        return origin;
    }

    public AirportCode destination() {
        return destination;
    }

    public Instant departureAt() {
        return departureAt;
    }

    public Instant arrivalAt() {
        return arrivalAt;
    }

    public Money price() {
        return price;
    }

    public int availableSeats() {
        return availableSeats;
    }
}

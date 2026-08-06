package com.travelplatform.flight.domain.flight;

import com.travelplatform.flight.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root for the flight bounded context. No lifecycle yet beyond creation and seat
 * reservation (no update/delete endpoint); revisit once a real need appears.
 */
public final class Flight {

    private final FlightId id;
    private final AirportCode origin;
    private final AirportCode destination;
    private final Instant departureAt;
    private final Instant arrivalAt;
    private final Money price;
    private final int availableSeats;
    private final String airline;
    private final String airlineCode;
    private final String flightNumber;
    private final String cabinClass;
    private final int stops;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Flight(
            FlightId id,
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
            int stops) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
        this.price = price;
        this.availableSeats = availableSeats;
        this.airline = airline;
        this.airlineCode = airlineCode;
        this.flightNumber = flightNumber;
        this.cabinClass = cabinClass;
        this.stops = stops;
    }

    public static Flight create(
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
            int stops) {
        if (!arrivalAt.isAfter(departureAt)) {
            throw new InvalidFlightScheduleException();
        }
        if (availableSeats < 0) {
            throw new IllegalArgumentException("availableSeats must not be negative");
        }
        if (stops < 0) {
            throw new IllegalArgumentException("stops must not be negative");
        }
        var flight =
                new Flight(
                        FlightId.newId(),
                        origin,
                        destination,
                        departureAt,
                        arrivalAt,
                        price,
                        availableSeats,
                        airline,
                        airlineCode,
                        flightNumber,
                        cabinClass,
                        stops);
        flight.domainEvents.add(
                new FlightCreated(
                        flight.id,
                        flight.origin,
                        flight.destination,
                        flight.departureAt,
                        flight.arrivalAt,
                        flight.price,
                        flight.availableSeats,
                        flight.airline,
                        flight.airlineCode,
                        flight.flightNumber,
                        flight.cabinClass,
                        flight.stops,
                        Instant.now()));
        return flight;
    }

    public static Flight reconstitute(
            FlightId id,
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
            int stops) {
        return new Flight(
                id,
                origin,
                destination,
                departureAt,
                arrivalAt,
                price,
                availableSeats,
                airline,
                airlineCode,
                flightNumber,
                cabinClass,
                stops);
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

    public String airline() {
        return airline;
    }

    public String airlineCode() {
        return airlineCode;
    }

    public String flightNumber() {
        return flightNumber;
    }

    public String cabinClass() {
        return cabinClass;
    }

    public int stops() {
        return stops;
    }

    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}

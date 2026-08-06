package com.travelplatform.flight.application.port.out;

import com.travelplatform.flight.domain.flight.AirportCode;
import com.travelplatform.flight.domain.flight.Flight;
import com.travelplatform.flight.domain.flight.FlightId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlightRepository {

    void save(Flight flight);

    Optional<Flight> findById(FlightId id);

    /** {@code departureDate} is optional - null means no date filter. */
    List<Flight> search(AirportCode origin, AirportCode destination, LocalDate departureDate);

    /**
     * Atomically decrements {@code availableSeats} by {@code quantity} if and only if enough seats
     * are available. Returns false (no-op) if the flight does not exist or has insufficient seats -
     * the caller decides what that means (retry, dead-letter, ...).
     */
    boolean tryReserve(FlightId id, int quantity);
}

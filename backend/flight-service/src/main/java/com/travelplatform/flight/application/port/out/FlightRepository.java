package com.travelplatform.flight.application.port.out;

import com.travelplatform.flight.domain.flight.AirportCode;
import com.travelplatform.flight.domain.flight.Flight;
import com.travelplatform.flight.domain.flight.FlightId;
import java.util.List;
import java.util.Optional;

public interface FlightRepository {

    void save(Flight flight);

    Optional<Flight> findById(FlightId id);

    List<Flight> search(AirportCode origin, AirportCode destination);
}

package com.travelplatform.flight.application.port.in;

import com.travelplatform.flight.domain.flight.Flight;
import java.time.LocalDate;
import java.util.List;

public interface SearchFlightsUseCase {

    List<Flight> search(SearchFlightsQuery query);

    /** {@code departureDate} is optional - null means "any date". */
    record SearchFlightsQuery(String origin, String destination, LocalDate departureDate) {}
}

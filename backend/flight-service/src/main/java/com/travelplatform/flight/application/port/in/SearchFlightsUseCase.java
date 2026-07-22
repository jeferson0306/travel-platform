package com.travelplatform.flight.application.port.in;

import com.travelplatform.flight.domain.flight.Flight;
import java.util.List;

public interface SearchFlightsUseCase {

    List<Flight> search(SearchFlightsQuery query);

    record SearchFlightsQuery(String origin, String destination) {}
}

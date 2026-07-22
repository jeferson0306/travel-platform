package com.travelplatform.flight.application.usecase;

import com.travelplatform.flight.application.port.in.SearchFlightsUseCase;
import com.travelplatform.flight.application.port.out.FlightRepository;
import com.travelplatform.flight.domain.flight.AirportCode;
import com.travelplatform.flight.domain.flight.Flight;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class SearchFlightsService implements SearchFlightsUseCase {

    private final FlightRepository flightRepository;

    public SearchFlightsService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public List<Flight> search(SearchFlightsQuery query) {
        return flightRepository.search(
                new AirportCode(query.origin()), new AirportCode(query.destination()));
    }
}

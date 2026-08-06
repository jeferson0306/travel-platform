package com.travelplatform.search.application.usecase;

import com.travelplatform.search.application.port.in.IndexFlightUseCase;
import com.travelplatform.search.application.port.out.FlightSearchRepository;
import com.travelplatform.search.domain.flight.SearchableFlight;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IndexFlightService implements IndexFlightUseCase {

    private final FlightSearchRepository flightSearchRepository;

    public IndexFlightService(FlightSearchRepository flightSearchRepository) {
        this.flightSearchRepository = flightSearchRepository;
    }

    @Override
    public void index(IndexFlightCommand command) {
        flightSearchRepository.index(
                new SearchableFlight(
                        command.flightId(),
                        command.origin(),
                        command.destination(),
                        command.departureAt(),
                        command.arrivalAt(),
                        command.priceAmount(),
                        command.priceCurrency(),
                        command.availableSeats()));
    }
}

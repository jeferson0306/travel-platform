package com.travelplatform.search.application.usecase;

import com.travelplatform.search.application.port.in.SearchFlightsUseCase;
import com.travelplatform.search.application.port.out.FlightSearchRepository;
import com.travelplatform.search.domain.flight.SearchableFlight;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class SearchFlightsService implements SearchFlightsUseCase {

    private final FlightSearchRepository flightSearchRepository;

    public SearchFlightsService(FlightSearchRepository flightSearchRepository) {
        this.flightSearchRepository = flightSearchRepository;
    }

    @Override
    public List<SearchableFlight> search(SearchFlightsQuery query) {
        if (query.prefix() != null && !query.prefix().isBlank()) {
            return flightSearchRepository.autocomplete(query.prefix());
        }
        return flightSearchRepository.searchByRoute(query.origin(), query.destination());
    }
}

package com.travelplatform.search.application.port.in;

import com.travelplatform.search.domain.flight.SearchableFlight;
import java.util.List;

public interface SearchFlightsUseCase {

    /**
     * Either {@code prefix} (autocomplete) or {@code origin}/{@code destination} (route search) is
     * expected to be set - see {@link
     * com.travelplatform.search.application.usecase.SearchFlightsService}.
     */
    List<SearchableFlight> search(SearchFlightsQuery query);

    record SearchFlightsQuery(String origin, String destination, String prefix) {}
}

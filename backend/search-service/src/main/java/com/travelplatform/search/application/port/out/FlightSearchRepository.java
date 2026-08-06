package com.travelplatform.search.application.port.out;

import com.travelplatform.search.domain.flight.SearchableFlight;
import java.util.List;

public interface FlightSearchRepository {

    /** Upsert by {@code flightId} - reindexing an already-indexed flight is a no-op overwrite. */
    void index(SearchableFlight flight);

    List<SearchableFlight> searchByRoute(String origin, String destination);

    /** Prefix match against origin/destination airport codes. */
    List<SearchableFlight> autocomplete(String prefix);
}

package com.travelplatform.search.api.resource;

import com.travelplatform.search.api.dto.FlightSearchResponse;
import com.travelplatform.search.api.dto.HotelSearchResponse;
import com.travelplatform.search.application.port.in.SearchFlightsUseCase;
import com.travelplatform.search.application.port.in.SearchFlightsUseCase.SearchFlightsQuery;
import com.travelplatform.search.application.port.in.SearchHotelsUseCase;
import com.travelplatform.search.application.port.in.SearchHotelsUseCase.SearchHotelsQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Fully public - no traveler needs an account to search, and this service has nothing to protect
 * (it holds no write endpoints; indexing only ever happens via the Kafka consumers). See
 * docs/adr/0012-search-service-opensearch.md.
 */
@Path("/api/v1/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    private final SearchFlightsUseCase searchFlightsUseCase;
    private final SearchHotelsUseCase searchHotelsUseCase;

    public SearchResource(
            SearchFlightsUseCase searchFlightsUseCase, SearchHotelsUseCase searchHotelsUseCase) {
        this.searchFlightsUseCase = searchFlightsUseCase;
        this.searchHotelsUseCase = searchHotelsUseCase;
    }

    @GET
    @Path("/flights")
    public List<FlightSearchResponse> searchFlights(
            @QueryParam("origin") String origin,
            @QueryParam("destination") String destination,
            @QueryParam("q") String prefix) {
        return searchFlightsUseCase
                .search(new SearchFlightsQuery(origin, destination, prefix))
                .stream()
                .map(FlightSearchResponse::from)
                .toList();
    }

    @GET
    @Path("/hotels")
    public List<HotelSearchResponse> searchHotels(
            @QueryParam("city") String city, @QueryParam("q") String prefix) {
        return searchHotelsUseCase.search(new SearchHotelsQuery(city, prefix)).stream()
                .map(HotelSearchResponse::from)
                .toList();
    }
}

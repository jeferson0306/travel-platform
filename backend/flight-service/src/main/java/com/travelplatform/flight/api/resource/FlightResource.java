package com.travelplatform.flight.api.resource;

import com.travelplatform.flight.api.dto.CreateFlightRequest;
import com.travelplatform.flight.api.dto.CreatedResponse;
import com.travelplatform.flight.api.dto.FlightResponse;
import com.travelplatform.flight.application.port.in.CreateFlightUseCase;
import com.travelplatform.flight.application.port.in.CreateFlightUseCase.CreateFlightCommand;
import com.travelplatform.flight.application.port.in.SearchFlightsUseCase;
import com.travelplatform.flight.application.port.in.SearchFlightsUseCase.SearchFlightsQuery;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;

/**
 * Search is public - no traveler needs an account to look up flights. Creating inventory is
 * restricted to staff roles (see docs/adr/0006-rbac-roles.md), enforced here by validating the JWT
 * issued by identity-service (RS256, verified with identity-service's public key - see the ADR's M9
 * addendum).
 */
@Path("/api/v1/flights")
@Produces(MediaType.APPLICATION_JSON)
public class FlightResource {

    private final CreateFlightUseCase createFlightUseCase;
    private final SearchFlightsUseCase searchFlightsUseCase;

    public FlightResource(
            CreateFlightUseCase createFlightUseCase, SearchFlightsUseCase searchFlightsUseCase) {
        this.createFlightUseCase = createFlightUseCase;
        this.searchFlightsUseCase = searchFlightsUseCase;
    }

    @GET
    public List<FlightResponse> search(
            @QueryParam("origin") String origin,
            @QueryParam("destination") String destination,
            @QueryParam("departureDate") String departureDate) {
        var date =
                departureDate == null || departureDate.isBlank()
                        ? null
                        : LocalDate.parse(departureDate);
        return searchFlightsUseCase
                .search(new SearchFlightsQuery(origin, destination, date))
                .stream()
                .map(FlightResponse::from)
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"MANAGER", "ADMIN", "SUPER_ADMIN"})
    public Response create(@Valid CreateFlightRequest request) {
        var id =
                createFlightUseCase.create(
                        new CreateFlightCommand(
                                request.origin(),
                                request.destination(),
                                request.departureAt(),
                                request.arrivalAt(),
                                request.priceAmount(),
                                request.priceCurrency(),
                                request.availableSeats()));
        return Response.status(Response.Status.CREATED)
                .entity(new CreatedResponse(id.value().toString()))
                .build();
    }
}

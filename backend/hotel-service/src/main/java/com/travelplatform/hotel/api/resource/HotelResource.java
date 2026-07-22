package com.travelplatform.hotel.api.resource;

import com.travelplatform.hotel.api.dto.CreateHotelRequest;
import com.travelplatform.hotel.api.dto.CreatedResponse;
import com.travelplatform.hotel.api.dto.HotelResponse;
import com.travelplatform.hotel.application.port.in.CreateHotelUseCase;
import com.travelplatform.hotel.application.port.in.CreateHotelUseCase.CreateHotelCommand;
import com.travelplatform.hotel.application.port.in.SearchHotelsUseCase;
import com.travelplatform.hotel.application.port.in.SearchHotelsUseCase.SearchHotelsQuery;
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
import java.util.List;

/**
 * Search is public. Creating inventory is restricted to staff roles (see
 * docs/adr/0006-rbac-roles.md), enforced by validating the JWT issued by identity-service.
 */
@Path("/api/v1/hotels")
@Produces(MediaType.APPLICATION_JSON)
public class HotelResource {

    private final CreateHotelUseCase createHotelUseCase;
    private final SearchHotelsUseCase searchHotelsUseCase;

    public HotelResource(
            CreateHotelUseCase createHotelUseCase, SearchHotelsUseCase searchHotelsUseCase) {
        this.createHotelUseCase = createHotelUseCase;
        this.searchHotelsUseCase = searchHotelsUseCase;
    }

    @GET
    public List<HotelResponse> search(@QueryParam("city") String city) {
        return searchHotelsUseCase.search(new SearchHotelsQuery(city)).stream()
                .map(HotelResponse::from)
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"MANAGER", "ADMIN", "SUPER_ADMIN"})
    public Response create(@Valid CreateHotelRequest request) {
        var id =
                createHotelUseCase.create(
                        new CreateHotelCommand(
                                request.name(),
                                request.city(),
                                request.pricePerNightAmount(),
                                request.pricePerNightCurrency(),
                                request.availableRooms()));
        return Response.status(Response.Status.CREATED)
                .entity(new CreatedResponse(id.value().toString()))
                .build();
    }
}

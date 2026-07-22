package com.travelplatform.booking.api.resource;

import com.travelplatform.booking.api.dto.CreateBookingRequest;
import com.travelplatform.booking.api.dto.CreatedResponse;
import com.travelplatform.booking.application.port.in.CancelBookingUseCase;
import com.travelplatform.booking.application.port.in.CancelBookingUseCase.CancelBookingCommand;
import com.travelplatform.booking.application.port.in.CreateBookingUseCase;
import com.travelplatform.booking.application.port.in.CreateBookingUseCase.CreateBookingCommand;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * travelerId is trusted client input for now - there is no Gateway/JWT enforcement in front of this
 * service yet (see docs/architecture/rate-limiting.md for the Gateway as the intended enforcement
 * point). Revisit once ROADMAP M14 lands.
 */
@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
public class BookingResource {

    private final CreateBookingUseCase createBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;

    public BookingResource(
            CreateBookingUseCase createBookingUseCase, CancelBookingUseCase cancelBookingUseCase) {
        this.createBookingUseCase = createBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateBookingRequest request) {
        var id =
                createBookingUseCase.create(
                        new CreateBookingCommand(
                                request.travelerId(),
                                request.travelerEmail(),
                                request.itemType(),
                                request.itemId(),
                                request.quantity(),
                                request.amount(),
                                request.currency()));
        return Response.status(Response.Status.CREATED)
                .entity(new CreatedResponse(id.value().toString()))
                .build();
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancel(@PathParam("id") String id) {
        cancelBookingUseCase.cancel(new CancelBookingCommand(id));
        return Response.noContent().build();
    }
}

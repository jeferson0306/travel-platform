package com.travelplatform.booking.api.resource;

import com.travelplatform.booking.api.dto.BookingResponse;
import com.travelplatform.booking.api.dto.CreateBookingRequest;
import com.travelplatform.booking.api.dto.CreatedResponse;
import com.travelplatform.booking.application.port.in.CancelBookingUseCase;
import com.travelplatform.booking.application.port.in.CancelBookingUseCase.CancelBookingCommand;
import com.travelplatform.booking.application.port.in.CreateBookingUseCase;
import com.travelplatform.booking.application.port.in.CreateBookingUseCase.CreateBookingCommand;
import com.travelplatform.booking.application.port.in.ListBookingsUseCase;
import com.travelplatform.booking.application.port.in.ListBookingsUseCase.ListBookingsQuery;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * travelerId is trusted client input for now, both for creating a booking and for listing a
 * traveler's own bookings below - there is no Gateway/JWT enforcement in front of this service yet
 * (the Gateway landed in ROADMAP M14 but only does signature/expiry fast-fail, not per-backend
 * authorization - see ARCHITECTURE.md's "defense in depth" note). A caller who knows another
 * traveler's id can currently list their bookings; verifying travelerId against the JWT subject
 * here is the fix, tracked as a known gap rather than silently left undocumented.
 */
@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
public class BookingResource {

    private final CreateBookingUseCase createBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final ListBookingsUseCase listBookingsUseCase;

    public BookingResource(
            CreateBookingUseCase createBookingUseCase,
            CancelBookingUseCase cancelBookingUseCase,
            ListBookingsUseCase listBookingsUseCase) {
        this.createBookingUseCase = createBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.listBookingsUseCase = listBookingsUseCase;
    }

    @GET
    public List<BookingResponse> listByTraveler(@QueryParam("travelerId") String travelerId) {
        return listBookingsUseCase.list(new ListBookingsQuery(travelerId)).stream()
                .map(BookingResponse::from)
                .toList();
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

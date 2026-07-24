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
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Every endpoint requires a valid JWT (verified against identity-service's public key - same RS256
 * setup as flight-service/hotel-service). travelerId is derived exclusively from the JWT subject,
 * never from client input - a caller can only create, list, or cancel their own bookings.
 * amount/currency/travelerEmail remain trusted client input (see CreateBookingRequest) - that's a
 * separate, still-open gap around authoritative pricing, not part of this fix.
 */
@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class BookingResource {

    private final CreateBookingUseCase createBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final ListBookingsUseCase listBookingsUseCase;
    private final JsonWebToken jwt;

    public BookingResource(
            CreateBookingUseCase createBookingUseCase,
            CancelBookingUseCase cancelBookingUseCase,
            ListBookingsUseCase listBookingsUseCase,
            JsonWebToken jwt) {
        this.createBookingUseCase = createBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.listBookingsUseCase = listBookingsUseCase;
        this.jwt = jwt;
    }

    @GET
    public List<BookingResponse> listByTraveler() {
        return listBookingsUseCase.list(new ListBookingsQuery(jwt.getSubject())).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateBookingRequest request) {
        var id =
                createBookingUseCase.create(
                        new CreateBookingCommand(
                                jwt.getSubject(),
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
        cancelBookingUseCase.cancel(new CancelBookingCommand(id, jwt.getSubject()));
        return Response.noContent().build();
    }
}

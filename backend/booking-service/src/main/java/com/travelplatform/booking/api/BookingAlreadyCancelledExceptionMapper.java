package com.travelplatform.booking.api;

import com.travelplatform.booking.api.dto.ErrorResponse;
import com.travelplatform.booking.domain.booking.BookingAlreadyCancelledException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BookingAlreadyCancelledExceptionMapper
        implements ExceptionMapper<BookingAlreadyCancelledException> {

    @Override
    public Response toResponse(BookingAlreadyCancelledException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("CONFLICT", exception.getMessage()))
                .build();
    }
}

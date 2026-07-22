package com.travelplatform.booking.api;

import com.travelplatform.booking.api.dto.ErrorResponse;
import com.travelplatform.booking.domain.booking.BookingNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BookingNotFoundExceptionMapper implements ExceptionMapper<BookingNotFoundException> {

    @Override
    public Response toResponse(BookingNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", exception.getMessage()))
                .build();
    }
}

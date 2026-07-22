package com.travelplatform.hotel.api;

import com.travelplatform.hotel.api.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Defense in depth: value-object validation (Money, HotelName, City) throws
 * IllegalArgumentException. DTO-level bean validation already catches most bad input before it
 * reaches the domain, but this ensures a domain-level rejection never leaks as a 500.
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("VALIDATION_ERROR", exception.getMessage()))
                .build();
    }
}

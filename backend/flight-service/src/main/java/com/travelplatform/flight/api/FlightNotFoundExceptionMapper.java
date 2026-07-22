package com.travelplatform.flight.api;

import com.travelplatform.flight.api.dto.ErrorResponse;
import com.travelplatform.flight.domain.flight.FlightNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FlightNotFoundExceptionMapper implements ExceptionMapper<FlightNotFoundException> {

    @Override
    public Response toResponse(FlightNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", exception.getMessage()))
                .build();
    }
}

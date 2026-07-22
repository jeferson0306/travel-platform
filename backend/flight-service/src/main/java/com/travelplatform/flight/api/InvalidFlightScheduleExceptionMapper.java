package com.travelplatform.flight.api;

import com.travelplatform.flight.api.dto.ErrorResponse;
import com.travelplatform.flight.domain.flight.InvalidFlightScheduleException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidFlightScheduleExceptionMapper
        implements ExceptionMapper<InvalidFlightScheduleException> {

    @Override
    public Response toResponse(InvalidFlightScheduleException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("VALIDATION_ERROR", exception.getMessage()))
                .build();
    }
}

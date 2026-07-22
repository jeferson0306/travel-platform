package com.travelplatform.identity.api;

import com.travelplatform.identity.api.dto.ErrorResponse;
import com.travelplatform.identity.domain.user.InvalidCredentialsException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidCredentialsExceptionMapper
        implements ExceptionMapper<InvalidCredentialsException> {

    @Override
    public Response toResponse(InvalidCredentialsException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("INVALID_CREDENTIALS", exception.getMessage()))
                .build();
    }
}

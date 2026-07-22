package com.travelplatform.identity.api;

import com.travelplatform.identity.api.dto.ErrorResponse;
import com.travelplatform.identity.domain.user.EmailAlreadyRegisteredException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class EmailAlreadyRegisteredExceptionMapper
        implements ExceptionMapper<EmailAlreadyRegisteredException> {

    @Override
    public Response toResponse(EmailAlreadyRegisteredException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("CONFLICT", exception.getMessage()))
                .build();
    }
}

package com.travelplatform.flight.api;

import com.travelplatform.flight.api.dto.ErrorResponse;
import io.quarkus.security.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Raised by @RolesAllowed when the caller is authenticated but lacks the required role. */
@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Override
    public Response toResponse(ForbiddenException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("FORBIDDEN", "Insufficient role"))
                .build();
    }
}

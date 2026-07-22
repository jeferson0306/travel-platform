package com.travelplatform.hotel.api;

import com.travelplatform.hotel.api.dto.ErrorResponse;
import io.quarkus.security.AuthenticationFailedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Raised by @RolesAllowed for a present-but-invalid JWT. See docs/architecture/error-handling.md.
 */
@Provider
public class UnauthenticatedExceptionMapper
        implements ExceptionMapper<AuthenticationFailedException> {

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("NO_TOKEN", "Authentication required"))
                .build();
    }
}

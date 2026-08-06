package com.travelplatform.booking.api;

import com.travelplatform.booking.api.dto.ErrorResponse;
import io.quarkus.security.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Raised either by the security layer (authenticated but insufficient role) or explicitly by
 * CancelBookingService when the caller doesn't own the booking - exception.getMessage() carries a
 * specific reason in the latter case, so it's echoed rather than replaced with a generic one.
 */
@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Override
    public Response toResponse(ForbiddenException exception) {
        var message = exception.getMessage() != null ? exception.getMessage() : "Insufficient role";
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("FORBIDDEN", message))
                .build();
    }
}

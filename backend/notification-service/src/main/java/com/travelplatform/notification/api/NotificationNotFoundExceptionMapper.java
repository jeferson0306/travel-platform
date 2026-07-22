package com.travelplatform.notification.api;

import com.travelplatform.notification.api.dto.ErrorResponse;
import com.travelplatform.notification.domain.notification.NotificationNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotificationNotFoundExceptionMapper
        implements ExceptionMapper<NotificationNotFoundException> {

    @Override
    public Response toResponse(NotificationNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", exception.getMessage()))
                .build();
    }
}

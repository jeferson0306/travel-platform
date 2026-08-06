package com.travelplatform.assistant.api;

import com.travelplatform.assistant.api.dto.ErrorResponse;
import com.travelplatform.assistant.domain.assistant.AssistantUnavailableException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AssistantUnavailableExceptionMapper
        implements ExceptionMapper<AssistantUnavailableException> {

    @Override
    public Response toResponse(AssistantUnavailableException exception) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(
                        new ErrorResponse(
                                "ASSISTANT_UNAVAILABLE", "Assistant backend is unavailable"))
                .build();
    }
}

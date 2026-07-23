package com.travelplatform.assistant.api.resource;

import com.travelplatform.assistant.api.dto.AskRequest;
import com.travelplatform.assistant.api.dto.AskResponse;
import com.travelplatform.assistant.application.port.in.AskAssistantUseCase;
import com.travelplatform.assistant.application.port.in.AskAssistantUseCase.AskAssistantQuery;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * An internal engineering tool, not a traveler-facing feature - restricted to the same non-USER
 * roles as payment-service/notification-service's read endpoints (ADR 0006), not because the data
 * is sensitive but because plain USER accounts have no reason to query it.
 */
@Path("/api/v1/assistant")
@Produces(MediaType.APPLICATION_JSON)
public class AssistantResource {

    private final AskAssistantUseCase askAssistantUseCase;

    public AssistantResource(AskAssistantUseCase askAssistantUseCase) {
        this.askAssistantUseCase = askAssistantUseCase;
    }

    @POST
    @Path("/ask")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"MANAGER", "ADMIN", "SUPER_ADMIN", "SUPPORT"})
    public AskResponse ask(@Valid AskRequest request) {
        var answer = askAssistantUseCase.ask(new AskAssistantQuery(request.question()));
        return AskResponse.from(answer);
    }
}

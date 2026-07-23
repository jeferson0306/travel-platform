package com.travelplatform.assistant.infrastructure.llm;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for Ollama's chat API - base URL from quarkus.rest-client.ollama.url.
 */
@RegisterRestClient(configKey = "ollama")
public interface OllamaRestClient {

    @POST
    @Path("/api/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    OllamaChatResponse chat(OllamaChatRequest request);
}

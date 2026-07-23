package com.travelplatform.assistant.infrastructure.llm;

import com.travelplatform.assistant.application.port.out.LlmPort;
import com.travelplatform.assistant.domain.assistant.AssistantUnavailableException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Ollama is a genuine synchronous external dependency (the same category ADR 0014 scoped fault
 * tolerance to for gateway->backend and search-service->OpenSearch) - a chat completion is a pure
 * read with no side effect on Ollama, so retrying a failed/timed-out call is safe, same reasoning
 * as search-service's OpenSearch read retries.
 */
@ApplicationScoped
public class OllamaLlmAdapter implements LlmPort {

    @RestClient OllamaRestClient client;

    @Inject
    @ConfigProperty(name = "assistant.ollama.model")
    String model;

    @Inject
    @ConfigProperty(name = "assistant.ollama.num-ctx")
    int numCtx;

    @Override
    @Timeout(65000)
    @Retry(maxRetries = 1, delay = 500)
    public String complete(String systemPrompt, String userQuestion) {
        var request =
                OllamaChatRequest.of(
                        model,
                        List.of(
                                new OllamaMessage("system", systemPrompt),
                                new OllamaMessage("user", userQuestion)),
                        numCtx);
        try {
            var response = client.chat(request);
            return response.message().content();
        } catch (RuntimeException e) {
            throw new AssistantUnavailableException(e);
        }
    }
}

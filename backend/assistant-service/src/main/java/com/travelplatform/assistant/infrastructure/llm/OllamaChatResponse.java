package com.travelplatform.assistant.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from POST /api/chat (stream=false). Ollama sends more fields; only message matters here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(OllamaMessage message) {}

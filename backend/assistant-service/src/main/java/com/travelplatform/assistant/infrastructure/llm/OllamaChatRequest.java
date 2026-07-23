package com.travelplatform.assistant.infrastructure.llm;

import java.util.List;
import java.util.Map;

/**
 * Body for POST /api/chat. stream=false - a single complete response, no SSE handling needed.
 * options.num_ctx is deliberately set (not left at Ollama's default 2048) - this service's whole
 * grounding corpus is ~7,000 tokens on its own, and a real test run without num_ctx set
 * demonstrated the failure mode directly: the model silently dropped the corpus and hallucinated an
 * answer about a completely unrelated platform instead of admitting it didn't know (ADR 0018).
 */
public record OllamaChatRequest(
        String model, List<OllamaMessage> messages, boolean stream, Map<String, Object> options) {

    public static OllamaChatRequest of(String model, List<OllamaMessage> messages, int numCtx) {
        return new OllamaChatRequest(model, messages, false, Map.of("num_ctx", numCtx));
    }
}

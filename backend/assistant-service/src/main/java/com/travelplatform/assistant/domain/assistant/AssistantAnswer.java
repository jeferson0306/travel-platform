package com.travelplatform.assistant.domain.assistant;

import java.util.List;

/**
 * The LLM's answer plus which corpus documents it was grounded in - sourcesUsed is always every
 * document in the corpus (ADR 0018's "stuff everything" approach has no retrieval step to narrow
 * it), returned so a caller can verify what grounding was actually available.
 */
public record AssistantAnswer(String text, List<String> sourcesUsed) {

    public AssistantAnswer {
        sourcesUsed = List.copyOf(sourcesUsed);
    }
}

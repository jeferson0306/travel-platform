package com.travelplatform.assistant.domain.assistant;

/**
 * One bundled markdown file from docs/context or docs/prompts (ROADMAP M18) - this service's entire
 * grounding corpus. sourceName is relative, e.g. "context/platform-overview.md".
 */
public record ContextDocument(String sourceName, String content) {

    public ContextDocument {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("sourceName must not be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
    }
}

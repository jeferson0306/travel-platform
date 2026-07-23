package com.travelplatform.assistant.domain.assistant;

/**
 * A question this service was asked. Value object so blank-question rejection lives in one place.
 */
public record AssistantQuestion(String text) {

    public AssistantQuestion {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
    }
}

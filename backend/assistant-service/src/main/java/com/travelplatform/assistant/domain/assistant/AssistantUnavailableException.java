package com.travelplatform.assistant.domain.assistant;

import com.travelplatform.assistant.domain.shared.DomainException;

/** Raised when Ollama (this service's one synchronous dependency, ADR 0018) cannot be reached. */
public class AssistantUnavailableException extends DomainException {

    public AssistantUnavailableException(Throwable cause) {
        super("Assistant backend unavailable: " + cause.getMessage());
    }
}

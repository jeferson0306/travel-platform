package com.travelplatform.assistant.application.port.out;

/** A chat-completion call to the underlying LLM (Ollama - ADR 0018). */
public interface LlmPort {

    String complete(String systemPrompt, String userQuestion);
}

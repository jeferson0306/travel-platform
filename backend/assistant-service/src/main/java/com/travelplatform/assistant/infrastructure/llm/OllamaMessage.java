package com.travelplatform.assistant.infrastructure.llm;

/** One message in Ollama's /api/chat request/response shape. */
public record OllamaMessage(String role, String content) {}

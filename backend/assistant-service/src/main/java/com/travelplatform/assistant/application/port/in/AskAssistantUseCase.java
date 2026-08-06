package com.travelplatform.assistant.application.port.in;

import com.travelplatform.assistant.domain.assistant.AssistantAnswer;

public interface AskAssistantUseCase {

    record AskAssistantQuery(String question) {}

    AssistantAnswer ask(AskAssistantQuery query);
}

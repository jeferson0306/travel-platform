package com.travelplatform.assistant.api.dto;

import com.travelplatform.assistant.domain.assistant.AssistantAnswer;
import java.util.List;

public record AskResponse(String answer, List<String> sourcesUsed) {

    public static AskResponse from(AssistantAnswer answer) {
        return new AskResponse(answer.text(), answer.sourcesUsed());
    }
}

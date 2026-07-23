package com.travelplatform.assistant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.assistant.application.port.in.AskAssistantUseCase.AskAssistantQuery;
import com.travelplatform.assistant.application.port.out.ContextCorpusPort;
import com.travelplatform.assistant.application.port.out.LlmPort;
import com.travelplatform.assistant.domain.assistant.ContextDocument;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AskAssistantService")
class AskAssistantServiceTest {

    @Test
    @DisplayName("grounds the LLM call in the whole corpus and reports every source used")
    void groundsInWholeCorpus() {
        var corpusPort = mock(ContextCorpusPort.class);
        var llmPort = mock(LlmPort.class);
        var corpus =
                List.of(
                        new ContextDocument(
                                "context/platform-overview.md", "Platform overview content"),
                        new ContextDocument("context/conventions.md", "Conventions content"));
        when(corpusPort.loadAll()).thenReturn(corpus);
        when(llmPort.complete(anyString(), anyString())).thenReturn("The answer");

        var service = new AskAssistantService(corpusPort, llmPort);
        var answer = service.ask(new AskAssistantQuery("What is this platform?"));

        assertThat(answer.text()).isEqualTo("The answer");
        assertThat(answer.sourcesUsed())
                .containsExactly("context/platform-overview.md", "context/conventions.md");

        var promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var questionCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(llmPort).complete(promptCaptor.capture(), questionCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Platform overview content")
                .contains("Conventions content");
        assertThat(questionCaptor.getValue()).isEqualTo("What is this platform?");
    }

    @Test
    @DisplayName("rejects a blank question before calling the LLM")
    void rejectsBlankQuestion() {
        var corpusPort = mock(ContextCorpusPort.class);
        var llmPort = mock(LlmPort.class);
        when(corpusPort.loadAll()).thenReturn(List.of());
        var service = new AskAssistantService(corpusPort, llmPort);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.ask(new AskAssistantQuery("   ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

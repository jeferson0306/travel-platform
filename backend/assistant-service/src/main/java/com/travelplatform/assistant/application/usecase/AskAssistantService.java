package com.travelplatform.assistant.application.usecase;

import com.travelplatform.assistant.application.port.in.AskAssistantUseCase;
import com.travelplatform.assistant.application.port.out.ContextCorpusPort;
import com.travelplatform.assistant.application.port.out.LlmPort;
import com.travelplatform.assistant.domain.assistant.AssistantAnswer;
import com.travelplatform.assistant.domain.assistant.AssistantQuestion;
import com.travelplatform.assistant.domain.assistant.ContextDocument;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * "Stuff everything" RAG (ADR 0018): the whole corpus is small enough (a handful of short Markdown
 * files) to include in full on every call, so there is no retrieval/ranking step to get wrong -
 * simpler and more accurate than chunking+embedding a corpus this size would be.
 */
@ApplicationScoped
public class AskAssistantService implements AskAssistantUseCase {

    private static final String INSTRUCTIONS =
            """
            You are the engineering assistant for the travel-platform repository, a Java/Quarkus \
            microservices platform. Answer ONLY using the reference material below - it is the \
            complete and authoritative context for this codebase. If the material does not cover \
            the question, say so explicitly instead of guessing. Be concise and technical; this \
            audience is engineers, not end users.

            """;

    private final LlmPort llmPort;
    private final List<ContextDocument> corpus;
    private final String systemPrompt;

    public AskAssistantService(ContextCorpusPort contextCorpusPort, LlmPort llmPort) {
        this.llmPort = llmPort;
        this.corpus = contextCorpusPort.loadAll();
        this.systemPrompt = buildSystemPrompt(corpus);
    }

    @Override
    public AssistantAnswer ask(AskAssistantQuery query) {
        var question = new AssistantQuestion(query.question());
        var answerText = llmPort.complete(systemPrompt, question.text());
        return new AssistantAnswer(
                answerText, corpus.stream().map(ContextDocument::sourceName).toList());
    }

    private static String buildSystemPrompt(List<ContextDocument> corpus) {
        var builder = new StringBuilder(INSTRUCTIONS);
        for (var doc : corpus) {
            builder.append("=== ").append(doc.sourceName()).append(" ===\n");
            builder.append(doc.content()).append("\n\n");
        }
        return builder.toString();
    }
}

package com.travelplatform.assistant.infrastructure.context;

import com.travelplatform.assistant.application.port.out.ContextCorpusPort;
import com.travelplatform.assistant.domain.assistant.ContextDocument;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reads the docs/context and docs/prompts files this service's pom.xml bundles as classpath
 * resources under "assistant-corpus/" at build time (ADR 0018). The filename list below is
 * deliberately explicit, not discovered by scanning the classpath at runtime - a plain JAR doesn't
 * expose directory listings without an extra library, and an explicit list is honest about the one
 * real cost of this approach: adding a new docs/context or docs/prompts file means adding its name
 * here too. Same procedural-accuracy trade-off as docs/context's own contract (ADR 0017) - a
 * missing entry is a bug to fix, not a reason to add scanning machinery.
 */
@ApplicationScoped
public class ClasspathContextCorpusRepository implements ContextCorpusPort {

    private static final List<String> RESOURCE_NAMES =
            List.of(
                    "context/README.md",
                    "context/platform-overview.md",
                    "context/service-catalog.md",
                    "context/event-catalog.md",
                    "context/conventions.md",
                    "prompts/README.md",
                    "prompts/milestone-workflow.md",
                    "prompts/new-microservice.md",
                    "prompts/new-endpoint.md",
                    "prompts/new-rabbitmq-consumer.md",
                    "prompts/new-adr.md");

    @Override
    public List<ContextDocument> loadAll() {
        return RESOURCE_NAMES.stream().map(this::load).toList();
    }

    private ContextDocument load(String resourceName) {
        var classpathPath = "assistant-corpus/" + resourceName;
        try (InputStream in =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathPath)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Missing bundled context resource: " + classpathPath);
            }
            return new ContextDocument(
                    resourceName, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read bundled context resource: " + classpathPath, e);
        }
    }
}

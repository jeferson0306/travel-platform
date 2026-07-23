package com.travelplatform.assistant.infrastructure.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the corpus this service's pom.xml bundles from docs/context and docs/prompts (ADR 0018)
 * is actually present and readable on the classpath - a plain unit test, not @QuarkusTest, since it
 * only depends on Maven's resource copy having run.
 */
@DisplayName("ClasspathContextCorpusRepository")
class ClasspathContextCorpusRepositoryTest {

    @Test
    @DisplayName("loads every bundled context and prompt document with non-blank content")
    void loadsEveryBundledDocument() {
        var repository = new ClasspathContextCorpusRepository();

        var corpus = repository.loadAll();

        assertThat(corpus).hasSize(11);
        assertThat(corpus).extracting(doc -> doc.content().isBlank()).containsOnly(false);
        assertThat(corpus)
                .extracting("sourceName")
                .contains("context/platform-overview.md", "prompts/new-microservice.md");
    }
}

package com.travelplatform.assistant.application.port.out;

import com.travelplatform.assistant.domain.assistant.ContextDocument;
import java.util.List;

/** Loads this service's grounding corpus - the bundled docs/context and docs/prompts files. */
public interface ContextCorpusPort {

    List<ContextDocument> loadAll();
}

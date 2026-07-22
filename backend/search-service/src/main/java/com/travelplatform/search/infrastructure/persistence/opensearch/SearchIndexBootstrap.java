package com.travelplatform.search.infrastructure.persistence.opensearch;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import org.jboss.logging.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;

/**
 * Creates the "flights"/"hotels" indices with explicit keyword/date/number mappings on startup, if
 * they do not already exist - the same "own the schema" spirit as every other service's MongoDB
 * document mapper, just expressed as an OpenSearch mapping instead. "retry_tasks"/"dead_letters"
 * are deliberately left to dynamic mapping - they are operational bookkeeping, never queried by
 * field type, so an explicit mapping would add ceremony without benefit.
 */
@ApplicationScoped
public class SearchIndexBootstrap {

    private static final Logger LOG = Logger.getLogger(SearchIndexBootstrap.class);

    private final OpenSearchClient client;

    public SearchIndexBootstrap(OpenSearchClient client) {
        this.client = client;
    }

    void onStart(@Observes StartupEvent event) {
        try {
            ensureIndex(
                    "flights",
                    new TypeMapping.Builder()
                            .properties("flightId", Property.of(p -> p.keyword(k -> k)))
                            .properties("origin", Property.of(p -> p.keyword(k -> k)))
                            .properties("destination", Property.of(p -> p.keyword(k -> k)))
                            .properties("departureAt", Property.of(p -> p.date(d -> d)))
                            .properties("arrivalAt", Property.of(p -> p.date(d -> d)))
                            .properties("priceAmount", Property.of(p -> p.double_(d -> d)))
                            .properties("priceCurrency", Property.of(p -> p.keyword(k -> k)))
                            .properties("availableSeats", Property.of(p -> p.integer(i -> i)))
                            .build());
            ensureIndex(
                    "hotels",
                    new TypeMapping.Builder()
                            .properties("hotelId", Property.of(p -> p.keyword(k -> k)))
                            .properties("name", Property.of(p -> p.keyword(k -> k)))
                            .properties("nameLower", Property.of(p -> p.keyword(k -> k)))
                            .properties("city", Property.of(p -> p.keyword(k -> k)))
                            .properties("cityLower", Property.of(p -> p.keyword(k -> k)))
                            .properties("pricePerNightAmount", Property.of(p -> p.double_(d -> d)))
                            .properties(
                                    "pricePerNightCurrency", Property.of(p -> p.keyword(k -> k)))
                            .properties("availableRooms", Property.of(p -> p.integer(i -> i)))
                            .build());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to bootstrap OpenSearch indices", e);
        }
    }

    private void ensureIndex(String name, TypeMapping mapping) throws IOException {
        if (client.indices().exists(e -> e.index(name)).value()) {
            return;
        }
        client.indices().create(c -> c.index(name).mappings(mapping));
        LOG.infof("Created OpenSearch index '%s'", name);
    }
}

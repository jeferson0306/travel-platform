package com.travelplatform.search.infrastructure.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

/**
 * OpenSearch-backed equivalent of the Mongo {@code retry_tasks}/{@code dead_letters} collections
 * used everywhere else in this platform - see docs/adr/0012-search-service-opensearch.md. Shared by
 * both {@link FlightCreatedConsumer}/{@link HotelCreatedConsumer} (write) and {@link RetryRelay}
 * (read, update, delete, dead-letter).
 */
@ApplicationScoped
public class RetryTaskStore {

    private static final String RETRY_TASKS_INDEX = "retry_tasks";
    private static final String DEAD_LETTERS_INDEX = "dead_letters";

    private final OpenSearchClient client;

    public RetryTaskStore(OpenSearchClient client) {
        this.client = client;
    }

    public record RetryTask(String id, RetryTaskDocument document) {}

    public void schedule(String eventType, String payload, String reason) {
        var id = UUID.randomUUID().toString();
        var document = new RetryTaskDocument(eventType, payload, 0, reason, Instant.now());
        index(id, document);
    }

    /**
     * Test-only escape hatch: fetches every retry task regardless of {@code nextAttemptAt}, so
     * tests can fast-forward a backed-off task instead of waiting out real time.
     */
    List<RetryTask> findAll() {
        try {
            SearchResponse<RetryTaskDocument> response =
                    client.search(s -> s.index(RETRY_TASKS_INDEX), RetryTaskDocument.class);
            return response.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(hit -> new RetryTask(hit.id(), hit.source()))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to query all retry tasks", e);
        }
    }

    public List<RetryTask> findDue() {
        var query =
                Query.of(
                        q ->
                                q.range(
                                        r ->
                                                r.field("nextAttemptAt")
                                                        .lte(json(Instant.now().toString()))));
        try {
            SearchResponse<RetryTaskDocument> response =
                    client.search(
                            s -> s.index(RETRY_TASKS_INDEX).query(query), RetryTaskDocument.class);
            return response.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(hit -> new RetryTask(hit.id(), hit.source()))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to query due retry tasks", e);
        }
    }

    public void delete(String id) {
        try {
            client.delete(d -> d.index(RETRY_TASKS_INDEX).id(id).refresh(Refresh.WaitFor));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete retry task " + id, e);
        }
    }

    public void reschedule(String id, RetryTaskDocument document) {
        index(id, document);
    }

    public void deadLetter(String id, RetryTaskDocument document) {
        try {
            client.index(
                    i ->
                            i.index(DEAD_LETTERS_INDEX)
                                    .id(id)
                                    .document(document)
                                    .refresh(Refresh.WaitFor));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to dead-letter retry task " + id, e);
        }
        delete(id);
    }

    private void index(String id, RetryTaskDocument document) {
        try {
            client.index(
                    i ->
                            i.index(RETRY_TASKS_INDEX)
                                    .id(id)
                                    .document(document)
                                    .refresh(Refresh.WaitFor));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write retry task " + id, e);
        }
    }

    private static org.opensearch.client.json.JsonData json(String value) {
        return org.opensearch.client.json.JsonData.of(value);
    }
}

package com.travelplatform.search.infrastructure.persistence.opensearch;

import com.travelplatform.search.application.port.out.FlightSearchRepository;
import com.travelplatform.search.domain.flight.SearchableFlight;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;

/**
 * Indexing by {@code flightId} is an upsert (see {@link FlightSearchRepository#index}) - a document
 * with the same id just overwrites the previous one, which is why this service needs no
 * processed-events claim collection unlike every Mongo-backed consumer elsewhere in the platform
 * (see docs/adr/0012-search-service-opensearch.md).
 *
 * <p>{@code @Timeout}/{@code @Retry} apply only to the read methods below, not {@link #index} -
 * that write path already has its own durable, Mongo-independent retry/DLQ mechanism ({@code
 * RetryRelay}); stacking SmallRye's in-process retry on top would be redundant at best and
 * conflicting at worst. Reads are pure and side-effect-free, so retrying them is always safe - see
 * docs/adr/0014-fault-tolerance.md.
 */
@ApplicationScoped
public class OpenSearchFlightSearchRepository implements FlightSearchRepository {

    private static final String INDEX = "flights";

    private final OpenSearchClient client;

    public OpenSearchFlightSearchRepository(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void index(SearchableFlight flight) {
        var document =
                new FlightSearchDocument(
                        flight.flightId(),
                        flight.origin(),
                        flight.destination(),
                        flight.departureAt(),
                        flight.arrivalAt(),
                        flight.priceAmount(),
                        flight.priceCurrency(),
                        flight.availableSeats());
        try {
            client.index(
                    i ->
                            i.index(INDEX)
                                    .id(flight.flightId())
                                    .document(document)
                                    .refresh(Refresh.WaitFor));
        } catch (IOException e) {
            throw new OpenSearchOperationException(
                    "Failed to index flight " + flight.flightId(), e);
        }
    }

    @Override
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 100)
    public List<SearchableFlight> searchByRoute(String origin, String destination) {
        var originTerm =
                Query.of(
                        q ->
                                q.term(
                                        t ->
                                                t.field("origin")
                                                        .value(
                                                                FieldValue.of(
                                                                        origin.toUpperCase(
                                                                                Locale.ROOT)))));
        var destinationTerm =
                Query.of(
                        q ->
                                q.term(
                                        t ->
                                                t.field("destination")
                                                        .value(
                                                                FieldValue.of(
                                                                        destination.toUpperCase(
                                                                                Locale.ROOT)))));
        var query = Query.of(q -> q.bool(b -> b.must(originTerm, destinationTerm)));
        return search(query, "route " + origin + "->" + destination);
    }

    @Override
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 100)
    public List<SearchableFlight> autocomplete(String prefix) {
        var normalized = prefix.toUpperCase(Locale.ROOT);
        var originPrefix = Query.of(q -> q.prefix(p -> p.field("origin").value(normalized)));
        var destinationPrefix =
                Query.of(q -> q.prefix(p -> p.field("destination").value(normalized)));
        var query = Query.of(q -> q.bool(b -> b.should(originPrefix, destinationPrefix)));
        return search(query, "autocomplete " + prefix);
    }

    private List<SearchableFlight> search(Query query, String description) {
        try {
            SearchResponse<FlightSearchDocument> response =
                    client.search(s -> s.index(INDEX).query(query), FlightSearchDocument.class);
            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .map(
                            d ->
                                    new SearchableFlight(
                                            d.flightId,
                                            d.origin,
                                            d.destination,
                                            d.departureAt,
                                            d.arrivalAt,
                                            d.priceAmount,
                                            d.priceCurrency,
                                            d.availableSeats))
                    .toList();
        } catch (IOException e) {
            throw new OpenSearchOperationException(
                    "Failed to search flights (" + description + ")", e);
        }
    }
}

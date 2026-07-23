package com.travelplatform.search.infrastructure.persistence.opensearch;

import com.travelplatform.search.application.port.out.HotelSearchRepository;
import com.travelplatform.search.domain.hotel.SearchableHotel;
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
 * Indexing by {@code hotelId} is an upsert (see {@link HotelSearchRepository#index}) - same
 * free-idempotency reasoning as {@link OpenSearchFlightSearchRepository}.
 * {@code @Timeout}/{@code @Retry} on the read methods only - same reasoning as {@link
 * OpenSearchFlightSearchRepository}, see docs/adr/0014-fault-tolerance.md.
 */
@ApplicationScoped
public class OpenSearchHotelSearchRepository implements HotelSearchRepository {

    private static final String INDEX = "hotels";

    private final OpenSearchClient client;

    public OpenSearchHotelSearchRepository(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void index(SearchableHotel hotel) {
        var document =
                new HotelSearchDocument(
                        hotel.hotelId(),
                        hotel.name(),
                        hotel.city(),
                        hotel.pricePerNightAmount(),
                        hotel.pricePerNightCurrency(),
                        hotel.availableRooms());
        try {
            client.index(
                    i ->
                            i.index(INDEX)
                                    .id(hotel.hotelId())
                                    .document(document)
                                    .refresh(Refresh.WaitFor));
        } catch (IOException e) {
            throw new OpenSearchOperationException("Failed to index hotel " + hotel.hotelId(), e);
        }
    }

    @Override
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 100)
    public List<SearchableHotel> searchByCity(String city) {
        var normalized = city.toLowerCase(Locale.ROOT);
        var query =
                Query.of(q -> q.term(t -> t.field("cityLower").value(FieldValue.of(normalized))));
        return search(query, "city " + city);
    }

    @Override
    @Timeout(3000)
    @Retry(maxRetries = 2, delay = 100)
    public List<SearchableHotel> autocomplete(String prefix) {
        var normalized = prefix.toLowerCase(Locale.ROOT);
        var namePrefix = Query.of(q -> q.prefix(p -> p.field("nameLower").value(normalized)));
        var cityPrefix = Query.of(q -> q.prefix(p -> p.field("cityLower").value(normalized)));
        var query = Query.of(q -> q.bool(b -> b.should(namePrefix, cityPrefix)));
        return search(query, "autocomplete " + prefix);
    }

    private List<SearchableHotel> search(Query query, String description) {
        try {
            SearchResponse<HotelSearchDocument> response =
                    client.search(s -> s.index(INDEX).query(query), HotelSearchDocument.class);
            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .map(
                            d ->
                                    new SearchableHotel(
                                            d.hotelId,
                                            d.name,
                                            d.city,
                                            d.pricePerNightAmount,
                                            d.pricePerNightCurrency,
                                            d.availableRooms))
                    .toList();
        } catch (IOException e) {
            throw new OpenSearchOperationException(
                    "Failed to search hotels (" + description + ")", e);
        }
    }
}

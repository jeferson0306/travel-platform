package com.travelplatform.search.domain.flight;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A flight as projected into the search index - not a rich aggregate, since this service owns no
 * business invariants of its own; flight-service is the source of truth (see
 * docs/adr/0012-search-service-opensearch.md). Construction still validates shape, the same way
 * every value object in this platform does.
 */
public record SearchableFlight(
        String flightId,
        String origin,
        String destination,
        Instant departureAt,
        Instant arrivalAt,
        BigDecimal priceAmount,
        String priceCurrency,
        int availableSeats) {

    public SearchableFlight {
        Objects.requireNonNull(flightId, "flightId must not be null");
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(departureAt, "departureAt must not be null");
        Objects.requireNonNull(arrivalAt, "arrivalAt must not be null");
        Objects.requireNonNull(priceAmount, "priceAmount must not be null");
        Objects.requireNonNull(priceCurrency, "priceCurrency must not be null");
    }
}

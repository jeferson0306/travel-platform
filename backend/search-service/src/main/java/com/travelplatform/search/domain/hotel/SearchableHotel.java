package com.travelplatform.search.domain.hotel;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A hotel as projected into the search index - not a rich aggregate, since this service owns no
 * business invariants of its own; hotel-service is the source of truth (see
 * docs/adr/0012-search-service-opensearch.md).
 */
public record SearchableHotel(
        String hotelId,
        String name,
        String city,
        BigDecimal pricePerNightAmount,
        String pricePerNightCurrency,
        int availableRooms) {

    public SearchableHotel {
        Objects.requireNonNull(hotelId, "hotelId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(city, "city must not be null");
        Objects.requireNonNull(pricePerNightAmount, "pricePerNightAmount must not be null");
        Objects.requireNonNull(pricePerNightCurrency, "pricePerNightCurrency must not be null");
    }
}

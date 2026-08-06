package com.travelplatform.search.infrastructure.persistence.opensearch;

import java.math.BigDecimal;

/**
 * Wire shape stored in the "hotels" OpenSearch index - the Jackson counterpart to {@link
 * com.travelplatform.search.domain.hotel.SearchableHotel}. {@code nameLower}/{@code cityLower} are
 * storage-only fields (not on the domain type) that exist purely so autocomplete can do a
 * case-insensitive prefix match without a custom analyzer - see
 * docs/adr/0012-search-service-opensearch.md.
 */
public class HotelSearchDocument {

    public String hotelId;
    public String name;
    public String nameLower;
    public String city;
    public String cityLower;
    public BigDecimal pricePerNightAmount;
    public String pricePerNightCurrency;
    public int availableRooms;

    public HotelSearchDocument() {}

    public HotelSearchDocument(
            String hotelId,
            String name,
            String city,
            BigDecimal pricePerNightAmount,
            String pricePerNightCurrency,
            int availableRooms) {
        this.hotelId = hotelId;
        this.name = name;
        this.nameLower = name.toLowerCase(java.util.Locale.ROOT);
        this.city = city;
        this.cityLower = city.toLowerCase(java.util.Locale.ROOT);
        this.pricePerNightAmount = pricePerNightAmount;
        this.pricePerNightCurrency = pricePerNightCurrency;
        this.availableRooms = availableRooms;
    }
}

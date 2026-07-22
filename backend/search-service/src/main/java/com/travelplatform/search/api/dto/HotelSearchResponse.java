package com.travelplatform.search.api.dto;

import com.travelplatform.search.domain.hotel.SearchableHotel;
import java.math.BigDecimal;

public record HotelSearchResponse(
        String hotelId,
        String name,
        String city,
        BigDecimal pricePerNightAmount,
        String pricePerNightCurrency,
        int availableRooms) {

    public static HotelSearchResponse from(SearchableHotel hotel) {
        return new HotelSearchResponse(
                hotel.hotelId(),
                hotel.name(),
                hotel.city(),
                hotel.pricePerNightAmount(),
                hotel.pricePerNightCurrency(),
                hotel.availableRooms());
    }
}

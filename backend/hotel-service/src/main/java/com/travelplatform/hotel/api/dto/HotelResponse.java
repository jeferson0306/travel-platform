package com.travelplatform.hotel.api.dto;

import com.travelplatform.hotel.domain.hotel.Hotel;
import java.math.BigDecimal;

public record HotelResponse(
        String id,
        String name,
        String city,
        BigDecimal pricePerNightAmount,
        String pricePerNightCurrency,
        int availableRooms) {

    public static HotelResponse from(Hotel hotel) {
        return new HotelResponse(
                hotel.id().value().toString(),
                hotel.name().value(),
                hotel.city().value(),
                hotel.pricePerNight().amount(),
                hotel.pricePerNight().currency(),
                hotel.availableRooms());
    }
}

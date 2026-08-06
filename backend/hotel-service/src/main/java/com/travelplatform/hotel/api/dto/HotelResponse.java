package com.travelplatform.hotel.api.dto;

import com.travelplatform.hotel.domain.hotel.Hotel;
import java.math.BigDecimal;
import java.util.List;

public record HotelResponse(
        String id,
        String name,
        String city,
        BigDecimal pricePerNightAmount,
        String pricePerNightCurrency,
        int availableRooms,
        String address,
        int starRating,
        List<String> amenities,
        String description,
        Double reviewScore,
        int reviewCount) {

    public static HotelResponse from(Hotel hotel) {
        return new HotelResponse(
                hotel.id().value().toString(),
                hotel.name().value(),
                hotel.city().value(),
                hotel.pricePerNight().amount(),
                hotel.pricePerNight().currency(),
                hotel.availableRooms(),
                hotel.address(),
                hotel.starRating(),
                hotel.amenities(),
                hotel.description(),
                hotel.reviewScore(),
                hotel.reviewCount());
    }
}

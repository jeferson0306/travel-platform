package com.travelplatform.hotel.application.port.in;

import com.travelplatform.hotel.domain.hotel.HotelId;
import java.math.BigDecimal;
import java.util.List;

public interface CreateHotelUseCase {

    HotelId create(CreateHotelCommand command);

    record CreateHotelCommand(
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
            int reviewCount) {}
}

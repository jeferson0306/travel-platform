package com.travelplatform.hotel.application.port.in;

import com.travelplatform.hotel.domain.hotel.HotelId;
import java.math.BigDecimal;

public interface CreateHotelUseCase {

    HotelId create(CreateHotelCommand command);

    record CreateHotelCommand(
            String name,
            String city,
            BigDecimal pricePerNightAmount,
            String pricePerNightCurrency,
            int availableRooms) {}
}

package com.travelplatform.search.application.port.in;

import java.math.BigDecimal;

/** Driven by hotel-service's hotel-created event (ROADMAP M13), not a public endpoint. */
public interface IndexHotelUseCase {

    void index(IndexHotelCommand command);

    record IndexHotelCommand(
            String hotelId,
            String name,
            String city,
            BigDecimal pricePerNightAmount,
            String pricePerNightCurrency,
            int availableRooms) {}
}

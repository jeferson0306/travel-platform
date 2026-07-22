package com.travelplatform.hotel.domain.hotel;

import com.travelplatform.hotel.domain.shared.DomainEvent;
import java.time.Instant;

/** Consumed by search-service to index this hotel (ROADMAP M13). */
public record HotelCreated(
        HotelId hotelId,
        HotelName name,
        City city,
        Money pricePerNight,
        int availableRooms,
        Instant occurredOn)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "hotel-created";
    }
}

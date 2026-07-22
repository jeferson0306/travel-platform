package com.travelplatform.booking.domain.booking;

import com.travelplatform.booking.domain.shared.DomainEvent;
import java.time.Instant;

public record BookingCreated(
        BookingId bookingId, TravelerId travelerId, BookingReference reference, Instant occurredOn)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "booking-created";
    }
}

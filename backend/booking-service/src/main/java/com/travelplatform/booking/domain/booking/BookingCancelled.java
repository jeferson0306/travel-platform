package com.travelplatform.booking.domain.booking;

import com.travelplatform.booking.domain.shared.DomainEvent;
import java.time.Instant;

public record BookingCancelled(BookingId bookingId, TravelerId travelerId, Instant occurredOn)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "booking-cancelled";
    }
}

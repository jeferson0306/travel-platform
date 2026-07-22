package com.travelplatform.booking.domain.booking;

import com.travelplatform.booking.domain.shared.DomainEvent;
import java.time.Instant;

public record BookingConfirmed(
        BookingId bookingId, TravelerId travelerId, Email travelerEmail, Instant occurredOn)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "booking-confirmed";
    }
}

package com.travelplatform.booking.domain.booking;

import com.travelplatform.booking.domain.shared.DomainException;

public final class BookingNotFoundException extends DomainException {

    public BookingNotFoundException(BookingId id) {
        super("Booking not found: " + id.value());
    }
}

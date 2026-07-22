package com.travelplatform.booking.domain.booking;

import com.travelplatform.booking.domain.shared.DomainException;

public final class BookingAlreadyCancelledException extends DomainException {

    public BookingAlreadyCancelledException(BookingId id) {
        super("Booking already cancelled: " + id.value());
    }
}

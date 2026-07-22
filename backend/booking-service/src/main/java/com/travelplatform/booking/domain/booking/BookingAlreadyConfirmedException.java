package com.travelplatform.booking.domain.booking;

import com.travelplatform.booking.domain.shared.DomainException;

/**
 * Raised by {@link Booking#confirm()} when the booking is already confirmed. The payment-authorized
 * consumer (ROADMAP M11) treats this as an idempotent no-op - Kafka's at-least-once delivery means
 * the same payment-authorized event can arrive twice.
 */
public final class BookingAlreadyConfirmedException extends DomainException {

    public BookingAlreadyConfirmedException(BookingId id) {
        super("Booking already confirmed: " + id.value());
    }
}

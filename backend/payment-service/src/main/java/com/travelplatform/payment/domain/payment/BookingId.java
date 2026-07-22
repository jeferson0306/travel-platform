package com.travelplatform.payment.domain.payment;

import java.util.Objects;
import java.util.UUID;

/**
 * The booking-service booking id this payment is for. A distinct type on purpose - this service
 * never shares a Java type with booking-service, only the UUID value carried on its events.
 */
public record BookingId(UUID value) {

    public BookingId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static BookingId of(String raw) {
        return new BookingId(UUID.fromString(raw));
    }
}

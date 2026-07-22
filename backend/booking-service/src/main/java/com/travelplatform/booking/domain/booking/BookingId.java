package com.travelplatform.booking.domain.booking;

import java.util.Objects;
import java.util.UUID;

public record BookingId(UUID value) {

    public BookingId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static BookingId newId() {
        return new BookingId(UUID.randomUUID());
    }

    public static BookingId of(String raw) {
        return new BookingId(UUID.fromString(raw));
    }
}

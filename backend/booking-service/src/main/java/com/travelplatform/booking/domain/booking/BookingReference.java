package com.travelplatform.booking.domain.booking;

import java.util.Objects;

/**
 * What is being booked. A freeform reference for now - flight-service and hotel-service (ROADMAP
 * M9) don't exist yet, so there is nothing structured to point at. Revisit once they do.
 */
public record BookingReference(String value) {

    private static final int MAX_LENGTH = 200;

    public BookingReference {
        Objects.requireNonNull(value, "value must not be null");
        value = value.strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "value must be at most " + MAX_LENGTH + " characters");
        }
    }
}

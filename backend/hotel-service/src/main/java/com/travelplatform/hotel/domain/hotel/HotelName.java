package com.travelplatform.hotel.domain.hotel;

import java.util.Objects;

public record HotelName(String value) {

    private static final int MAX_LENGTH = 200;

    public HotelName {
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

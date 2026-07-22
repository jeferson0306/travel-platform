package com.travelplatform.hotel.domain.hotel;

import java.util.Objects;

/** Freeform city name for now - no reference geo/city data source exists yet. */
public record City(String value) {

    private static final int MAX_LENGTH = 100;

    public City {
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

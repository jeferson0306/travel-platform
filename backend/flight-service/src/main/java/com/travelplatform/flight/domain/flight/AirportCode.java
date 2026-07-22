package com.travelplatform.flight.domain.flight;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** IATA airport code, e.g. "LIS", "GRU". */
public record AirportCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{3}$");

    public AirportCode {
        Objects.requireNonNull(value, "value must not be null");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid IATA airport code: " + value);
        }
    }
}

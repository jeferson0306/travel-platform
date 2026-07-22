package com.travelplatform.booking.domain.booking;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The traveler's email, carried so notification-service (ROADMAP M12) has an address to send
 * confirmations to without a synchronous call back to identity-service - the same trusted-client-
 * input simplification already applied to {@code amount} on {@link BookingReference}, see
 * CreateBookingRequest. Duplicated per service on purpose, not shared with identity-service's own
 * {@code Email} type (ADR 0005 - no shared runtime code between services).
 */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(value, "value must not be null");
        value = value.strip().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
}

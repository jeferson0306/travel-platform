package com.travelplatform.booking.domain.booking;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity-service user id that owns this booking. A distinct type on purpose - this service
 * never shares a Java type with identity-service, only the UUID value carried in the JWT subject.
 */
public record TravelerId(UUID value) {

    public TravelerId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static TravelerId of(String raw) {
        return new TravelerId(UUID.fromString(raw));
    }
}

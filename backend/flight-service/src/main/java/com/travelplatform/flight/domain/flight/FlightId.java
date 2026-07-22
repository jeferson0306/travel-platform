package com.travelplatform.flight.domain.flight;

import java.util.Objects;
import java.util.UUID;

public record FlightId(UUID value) {

    public FlightId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static FlightId newId() {
        return new FlightId(UUID.randomUUID());
    }

    public static FlightId of(String raw) {
        return new FlightId(UUID.fromString(raw));
    }
}

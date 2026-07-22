package com.travelplatform.hotel.domain.hotel;

import java.util.Objects;
import java.util.UUID;

public record HotelId(UUID value) {

    public HotelId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static HotelId newId() {
        return new HotelId(UUID.randomUUID());
    }

    public static HotelId of(String raw) {
        return new HotelId(UUID.fromString(raw));
    }
}

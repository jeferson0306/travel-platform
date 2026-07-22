package com.travelplatform.booking.domain.booking;

import java.util.Objects;

/**
 * What is being booked: a specific flight or hotel item, and how many (seats/rooms). Structured
 * since flight-service and hotel-service exist (ROADMAP M9) - carried on {@link BookingCreated} so
 * their inventory consumers (ROADMAP M10) know exactly what to decrement.
 */
public record BookingReference(ItemType itemType, String itemId, int quantity) {

    public BookingReference {
        Objects.requireNonNull(itemType, "itemType must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        itemId = itemId.strip();
        if (itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
    }
}

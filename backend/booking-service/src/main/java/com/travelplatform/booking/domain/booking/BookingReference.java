package com.travelplatform.booking.domain.booking;

import java.util.Objects;

/**
 * What is being booked: a specific flight or hotel item, and how many (seats/rooms). Structured
 * since flight-service and hotel-service exist (ROADMAP M9) - carried on {@link BookingCreated} so
 * their inventory consumers (ROADMAP M10) know exactly what to decrement.
 *
 * <p>itemSummary is an optional, human-readable description of the item (e.g. "Lisbon -> Sao Paulo,
 * TP123, TAP Air Portugal" for a flight, or "Lisbon Riverside Hotel, Lisbon" for a hotel). It is
 * trusted client input - the same rationale as amount/currency/travelerEmail on {@link
 * com.travelplatform.booking.api.dto.CreateBookingRequest} - and carries no domain-level validation
 * weight; it exists purely so the frontend can render a readable booking without booking-service
 * performing a synchronous lookup against flight-service/hotel-service, which ADR 0004 forbids.
 */
public record BookingReference(ItemType itemType, String itemId, int quantity, String itemSummary) {

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
        if (itemSummary != null) {
            itemSummary = itemSummary.strip();
            if (itemSummary.isBlank()) {
                itemSummary = null;
            }
        }
    }
}

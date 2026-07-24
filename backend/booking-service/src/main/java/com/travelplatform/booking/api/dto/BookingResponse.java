package com.travelplatform.booking.api.dto;

import com.travelplatform.booking.domain.booking.Booking;
import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(
        String id,
        String itemType,
        String itemId,
        int quantity,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.id().value().toString(),
                booking.reference().itemType().name(),
                booking.reference().itemId(),
                booking.reference().quantity(),
                booking.amount().amount(),
                booking.amount().currency(),
                booking.status().name(),
                booking.createdAt());
    }
}

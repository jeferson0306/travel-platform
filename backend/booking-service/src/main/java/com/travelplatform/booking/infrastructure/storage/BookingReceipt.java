package com.travelplatform.booking.infrastructure.storage;

import com.travelplatform.booking.domain.booking.Booking;
import java.time.Instant;

/** The JSON document written to S3 for a booking - see docs/adr/0009-booking-receipts-in-s3.md. */
public record BookingReceipt(
        String bookingId,
        String travelerId,
        String itemType,
        String itemId,
        int quantity,
        String amount,
        String currency,
        Instant createdAt) {

    public static BookingReceipt from(Booking booking) {
        return new BookingReceipt(
                booking.id().value().toString(),
                booking.travelerId().value().toString(),
                booking.reference().itemType().name(),
                booking.reference().itemId(),
                booking.reference().quantity(),
                booking.amount().amount().toPlainString(),
                booking.amount().currency(),
                booking.createdAt());
    }
}

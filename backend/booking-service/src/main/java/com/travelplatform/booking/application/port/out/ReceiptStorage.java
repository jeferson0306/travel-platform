package com.travelplatform.booking.application.port.out;

import com.travelplatform.booking.domain.booking.Booking;

/**
 * Writes a durable, traveler-facing receipt for a booking - see
 * docs/adr/0009-booking-receipts-in-s3.md. Best-effort: a failure here must never fail booking
 * creation, so this port has no return value or checked exception for the caller to react to.
 */
public interface ReceiptStorage {

    void store(Booking booking);
}

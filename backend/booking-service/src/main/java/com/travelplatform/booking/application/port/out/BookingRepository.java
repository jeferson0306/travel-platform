package com.travelplatform.booking.application.port.out;

import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingId;
import java.util.Optional;

/**
 * Outbound port for booking persistence. The implementing adapter also owns writing this
 * aggregate's pulled domain events to the transactional outbox, atomically with the booking write -
 * see docs/adr/0007-transactional-outbox.md for why that isn't a separate port.
 */
public interface BookingRepository {

    void save(Booking booking);

    Optional<Booking> findById(BookingId id);
}

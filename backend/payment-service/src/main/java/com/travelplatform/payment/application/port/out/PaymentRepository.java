package com.travelplatform.payment.application.port.out;

import com.travelplatform.payment.domain.payment.BookingId;
import com.travelplatform.payment.domain.payment.Payment;
import java.util.Optional;

/**
 * Outbound port for payment persistence. The implementing adapter also owns writing this
 * aggregate's pulled domain events to the transactional outbox, atomically with the payment write -
 * see docs/adr/0007-transactional-outbox.md (booking-service) for why that isn't a separate port.
 */
public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findByBookingId(BookingId bookingId);
}

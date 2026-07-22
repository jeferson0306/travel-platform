package com.travelplatform.payment.domain.payment;

import com.travelplatform.payment.domain.shared.DomainEvent;
import java.time.Instant;

public record PaymentFailed(
        PaymentId paymentId, BookingId bookingId, Money amount, String reason, Instant occurredOn)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "payment-failed";
    }
}

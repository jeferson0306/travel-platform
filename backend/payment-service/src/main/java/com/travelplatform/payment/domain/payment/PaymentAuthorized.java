package com.travelplatform.payment.domain.payment;

import com.travelplatform.payment.domain.shared.DomainEvent;
import java.time.Instant;

public record PaymentAuthorized(
        PaymentId paymentId, BookingId bookingId, Money amount, Instant occurredOn)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "payment-authorized";
    }
}

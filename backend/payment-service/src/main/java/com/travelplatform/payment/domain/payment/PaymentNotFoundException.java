package com.travelplatform.payment.domain.payment;

import com.travelplatform.payment.domain.shared.DomainException;

public final class PaymentNotFoundException extends DomainException {

    public PaymentNotFoundException(BookingId bookingId) {
        super("No payment found for booking: " + bookingId.value());
    }
}

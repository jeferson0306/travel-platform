package com.travelplatform.payment.domain.payment;

import com.travelplatform.payment.domain.shared.DomainException;

public final class PaymentAlreadyRefundedException extends DomainException {

    public PaymentAlreadyRefundedException(PaymentId id) {
        super("Payment already refunded: " + id.value());
    }
}

package com.travelplatform.payment.application.port.out;

import com.travelplatform.payment.domain.payment.Money;
import com.travelplatform.payment.domain.payment.PaymentId;

/**
 * The actual payment provider integration - simulated for now (see
 * infrastructure.gateway.SimulatedPaymentGateway and docs/adr/0010-payment-saga.md). A real
 * provider (Stripe) integration is a drop-in adapter behind this same port.
 */
public interface PaymentGateway {

    boolean authorize(Money amount);

    void refund(PaymentId paymentId, Money amount);
}

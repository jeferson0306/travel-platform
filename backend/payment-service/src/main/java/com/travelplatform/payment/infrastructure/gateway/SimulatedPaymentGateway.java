package com.travelplatform.payment.infrastructure.gateway;

import com.travelplatform.payment.application.port.out.PaymentGateway;
import com.travelplatform.payment.domain.payment.Money;
import com.travelplatform.payment.domain.payment.PaymentId;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Stands in for a real payment provider (Stripe) integration - see docs/adr/0010-payment-saga.md.
 * Always approves/succeeds: the point of this milestone is the saga/idempotency/outbox mechanics
 * around a payment, not a real charge. Swapping this for a real provider is a drop-in adapter
 * change behind {@link PaymentGateway} - nothing else in this service would need to change.
 */
@ApplicationScoped
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final Logger LOG = Logger.getLogger(SimulatedPaymentGateway.class);

    @Override
    public boolean authorize(Money amount) {
        LOG.infof("Simulated authorization for %s %s", amount.amount(), amount.currency());
        return true;
    }

    @Override
    public void refund(PaymentId paymentId, Money amount) {
        LOG.infof(
                "Simulated refund of %s %s for payment %s",
                amount.amount(), amount.currency(), paymentId.value());
    }
}

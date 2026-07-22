package com.travelplatform.payment.domain.payment;

import com.travelplatform.payment.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root for the payment bounded context. One payment per booking - see
 * docs/adr/0010-payment-saga.md. Consumer-level idempotency (see infrastructure.messaging) decides
 * whether {@link #authorize}/{@link #fail} is even called for a given bookingId; this aggregate
 * enforces the narrower invariant that a given payment cannot be refunded twice.
 */
public final class Payment {

    private final PaymentId id;
    private final BookingId bookingId;
    private final Money amount;
    private PaymentStatus status;
    private final Instant createdAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Payment(
            PaymentId id,
            BookingId bookingId,
            Money amount,
            PaymentStatus status,
            Instant createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Records a successful authorization, raising a {@link PaymentAuthorized} domain event. */
    public static Payment authorize(BookingId bookingId, Money amount) {
        var createdAt = Instant.now();
        var payment =
                new Payment(
                        PaymentId.newId(), bookingId, amount, PaymentStatus.AUTHORIZED, createdAt);
        payment.domainEvents.add(new PaymentAuthorized(payment.id, bookingId, amount, createdAt));
        return payment;
    }

    /** Records a declined authorization, raising a {@link PaymentFailed} domain event. */
    public static Payment fail(BookingId bookingId, Money amount, String reason) {
        var createdAt = Instant.now();
        var payment =
                new Payment(PaymentId.newId(), bookingId, amount, PaymentStatus.FAILED, createdAt);
        payment.domainEvents.add(
                new PaymentFailed(payment.id, bookingId, amount, reason, createdAt));
        return payment;
    }

    /** Rebuilds a payment from persisted state. Does not raise domain events. */
    public static Payment reconstitute(
            PaymentId id,
            BookingId bookingId,
            Money amount,
            PaymentStatus status,
            Instant createdAt) {
        return new Payment(id, bookingId, amount, status, createdAt);
    }

    /** Refunds an authorized payment, raising a {@link PaymentRefunded} domain event. */
    public void refund() {
        if (status == PaymentStatus.REFUNDED) {
            throw new PaymentAlreadyRefundedException(id);
        }
        if (status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                    "Only an authorized payment can be refunded: " + id.value());
        }
        status = PaymentStatus.REFUNDED;
        domainEvents.add(new PaymentRefunded(id, bookingId, amount, Instant.now()));
    }

    public PaymentId id() {
        return id;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public Money amount() {
        return amount;
    }

    public PaymentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** Returns and clears the domain events raised since this instance was created. */
    public List<DomainEvent> pullDomainEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}

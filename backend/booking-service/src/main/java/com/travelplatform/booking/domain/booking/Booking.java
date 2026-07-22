package com.travelplatform.booking.domain.booking;

import com.travelplatform.booking.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Aggregate root for the booking bounded context. */
public final class Booking {

    private final BookingId id;
    private final TravelerId travelerId;
    private final Email travelerEmail;
    private final BookingReference reference;
    private final Money amount;
    private BookingStatus status;
    private final Instant createdAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Booking(
            BookingId id,
            TravelerId travelerId,
            Email travelerEmail,
            BookingReference reference,
            Money amount,
            BookingStatus status,
            Instant createdAt) {
        this.id = id;
        this.travelerId = travelerId;
        this.travelerEmail = travelerEmail;
        this.reference = reference;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Creates a new booking, recording a {@link BookingCreated} domain event. */
    public static Booking create(
            TravelerId travelerId, Email travelerEmail, BookingReference reference, Money amount) {
        var createdAt = Instant.now();
        var booking =
                new Booking(
                        BookingId.newId(),
                        travelerId,
                        travelerEmail,
                        reference,
                        amount,
                        BookingStatus.PENDING,
                        createdAt);
        booking.domainEvents.add(
                new BookingCreated(
                        booking.id,
                        booking.travelerId,
                        booking.reference,
                        booking.amount,
                        createdAt));
        return booking;
    }

    /** Rebuilds a booking from persisted state. Does not raise domain events. */
    public static Booking reconstitute(
            BookingId id,
            TravelerId travelerId,
            Email travelerEmail,
            BookingReference reference,
            Money amount,
            BookingStatus status,
            Instant createdAt) {
        return new Booking(id, travelerId, travelerEmail, reference, amount, status, createdAt);
    }

    /** Cancels the booking, recording a {@link BookingCancelled} domain event. */
    public void cancel() {
        if (status == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException(id);
        }
        status = BookingStatus.CANCELLED;
        domainEvents.add(new BookingCancelled(id, travelerId, Instant.now()));
    }

    /**
     * Confirms the booking once payment-service reports the payment authorized (ROADMAP M11),
     * recording a {@link BookingConfirmed} domain event - notification-service (ROADMAP M12)
     * consumes it to send the confirmation email.
     */
    public void confirm() {
        if (status == BookingStatus.CONFIRMED) {
            throw new BookingAlreadyConfirmedException(id);
        }
        status = BookingStatus.CONFIRMED;
        domainEvents.add(new BookingConfirmed(id, travelerId, travelerEmail, Instant.now()));
    }

    public BookingId id() {
        return id;
    }

    public TravelerId travelerId() {
        return travelerId;
    }

    public Email travelerEmail() {
        return travelerEmail;
    }

    public BookingReference reference() {
        return reference;
    }

    public Money amount() {
        return amount;
    }

    public BookingStatus status() {
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

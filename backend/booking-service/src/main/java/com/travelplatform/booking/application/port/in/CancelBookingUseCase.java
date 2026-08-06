package com.travelplatform.booking.application.port.in;

public interface CancelBookingUseCase {

    void cancel(CancelBookingCommand command);

    /**
     * {@code callerTravelerId} is the authenticated JWT subject when cancellation is HTTP-initiated
     * (BookingResource) - only the booking's own traveler may cancel it that way. {@code null}
     * means a system-initiated cancellation (the payment-failed saga compensation via
     * PaymentFailedConsumer/RetryRelay) - those are already implicitly authorized by being
     * internal, not user-triggered, so no ownership check applies.
     */
    record CancelBookingCommand(String bookingId, String callerTravelerId) {}
}

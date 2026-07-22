package com.travelplatform.payment.application.port.in;

/** Driven by booking-service's booking-cancelled event (ROADMAP M11), not a public endpoint. */
public interface RefundPaymentUseCase {

    void refund(RefundPaymentCommand command);

    record RefundPaymentCommand(String bookingId) {}
}

package com.travelplatform.notification.application.port.in;

/** Driven by booking-service's booking-confirmed event (ROADMAP M12), not a public endpoint. */
public interface SendBookingConfirmationUseCase {

    void send(SendBookingConfirmationCommand command);

    record SendBookingConfirmationCommand(String bookingId, String recipientEmail) {}
}

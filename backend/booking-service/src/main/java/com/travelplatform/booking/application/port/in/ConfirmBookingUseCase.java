package com.travelplatform.booking.application.port.in;

public interface ConfirmBookingUseCase {

    void confirm(ConfirmBookingCommand command);

    record ConfirmBookingCommand(String bookingId) {}
}

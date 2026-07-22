package com.travelplatform.booking.application.port.in;

public interface CancelBookingUseCase {

    void cancel(CancelBookingCommand command);

    record CancelBookingCommand(String bookingId) {}
}

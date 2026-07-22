package com.travelplatform.booking.application.port.in;

import com.travelplatform.booking.domain.booking.BookingId;

public interface CreateBookingUseCase {

    BookingId create(CreateBookingCommand command);

    record CreateBookingCommand(String travelerId, String itemType, String itemId, int quantity) {}
}

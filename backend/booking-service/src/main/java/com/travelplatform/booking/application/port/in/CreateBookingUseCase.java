package com.travelplatform.booking.application.port.in;

import com.travelplatform.booking.domain.booking.BookingId;
import java.math.BigDecimal;

public interface CreateBookingUseCase {

    BookingId create(CreateBookingCommand command);

    record CreateBookingCommand(
            String travelerId,
            String travelerEmail,
            String itemType,
            String itemId,
            int quantity,
            BigDecimal amount,
            String currency) {}
}

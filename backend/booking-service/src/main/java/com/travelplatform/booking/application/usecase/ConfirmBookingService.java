package com.travelplatform.booking.application.usecase;

import com.travelplatform.booking.application.port.in.ConfirmBookingUseCase;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.BookingId;
import com.travelplatform.booking.domain.booking.BookingNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;

/** Driven by payment-service's payment-authorized event (ROADMAP M11), not a public endpoint. */
@ApplicationScoped
public class ConfirmBookingService implements ConfirmBookingUseCase {

    private final BookingRepository bookingRepository;

    public ConfirmBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void confirm(ConfirmBookingCommand command) {
        var id = BookingId.of(command.bookingId());
        var booking =
                bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException(id));
        booking.confirm();
        bookingRepository.save(booking);
    }
}

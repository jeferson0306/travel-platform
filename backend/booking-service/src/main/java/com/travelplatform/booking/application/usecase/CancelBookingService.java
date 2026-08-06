package com.travelplatform.booking.application.usecase;

import com.travelplatform.booking.application.port.in.CancelBookingUseCase;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.BookingId;
import com.travelplatform.booking.domain.booking.BookingNotFoundException;
import com.travelplatform.booking.domain.booking.TravelerId;
import io.quarkus.security.ForbiddenException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CancelBookingService implements CancelBookingUseCase {

    private final BookingRepository bookingRepository;

    public CancelBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void cancel(CancelBookingCommand command) {
        var id = BookingId.of(command.bookingId());
        var booking =
                bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException(id));
        if (command.callerTravelerId() != null
                && !booking.travelerId().equals(TravelerId.of(command.callerTravelerId()))) {
            throw new ForbiddenException("You can only cancel your own bookings");
        }
        booking.cancel();
        bookingRepository.save(booking);
    }
}

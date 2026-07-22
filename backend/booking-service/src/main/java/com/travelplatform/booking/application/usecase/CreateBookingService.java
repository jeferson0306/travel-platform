package com.travelplatform.booking.application.usecase;

import com.travelplatform.booking.application.port.in.CreateBookingUseCase;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingId;
import com.travelplatform.booking.domain.booking.BookingReference;
import com.travelplatform.booking.domain.booking.TravelerId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CreateBookingService implements CreateBookingUseCase {

    private final BookingRepository bookingRepository;

    public CreateBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public BookingId create(CreateBookingCommand command) {
        var booking =
                Booking.create(
                        TravelerId.of(command.travelerId()),
                        new BookingReference(command.reference()));
        bookingRepository.save(booking);
        return booking.id();
    }
}

package com.travelplatform.booking.application.usecase;

import com.travelplatform.booking.application.port.in.ListBookingsUseCase;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.TravelerId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ListBookingsService implements ListBookingsUseCase {

    private final BookingRepository bookingRepository;

    public ListBookingsService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<Booking> list(ListBookingsQuery query) {
        return bookingRepository.findByTravelerId(TravelerId.of(query.travelerId()));
    }
}

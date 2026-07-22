package com.travelplatform.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.booking.application.port.in.CancelBookingUseCase.CancelBookingCommand;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingNotFoundException;
import com.travelplatform.booking.domain.booking.BookingReference;
import com.travelplatform.booking.domain.booking.TravelerId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelBookingServiceTest {

    @Mock BookingRepository bookingRepository;

    CancelBookingService service;

    @BeforeEach
    void setUp() {
        service = new CancelBookingService(bookingRepository);
    }

    @Test
    void cancelsAnExistingBooking() {
        var booking =
                Booking.create(
                        new TravelerId(UUID.randomUUID()), new BookingReference("hotel-lisbon"));
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        service.cancel(new CancelBookingCommand(booking.id().value().toString()));

        verify(bookingRepository).save(booking);
    }

    @Test
    void rejectsCancellingAnUnknownBooking() {
        var id = UUID.randomUUID().toString();
        when(bookingRepository.findById(com.travelplatform.booking.domain.booking.BookingId.of(id)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(new CancelBookingCommand(id)))
                .isInstanceOf(BookingNotFoundException.class);
    }
}

package com.travelplatform.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.travelplatform.booking.application.port.in.CreateBookingUseCase.CreateBookingCommand;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.Booking;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateBookingServiceTest {

    @Mock BookingRepository bookingRepository;

    CreateBookingService service;

    @BeforeEach
    void setUp() {
        service = new CreateBookingService(bookingRepository);
    }

    @Test
    void createsAndPersistsABooking() {
        var travelerId = UUID.randomUUID().toString();

        var bookingId = service.create(new CreateBookingCommand(travelerId, "flight-LIS-GRU"));

        assertThat(bookingId).isNotNull();
        verify(bookingRepository).save(any(Booking.class));
    }
}

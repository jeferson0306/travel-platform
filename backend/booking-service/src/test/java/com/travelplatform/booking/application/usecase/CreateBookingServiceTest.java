package com.travelplatform.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.travelplatform.booking.application.port.in.CreateBookingUseCase.CreateBookingCommand;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.application.port.out.ReceiptStorage;
import com.travelplatform.booking.domain.booking.Booking;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateBookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock ReceiptStorage receiptStorage;

    CreateBookingService service;

    @BeforeEach
    void setUp() {
        service = new CreateBookingService(bookingRepository, receiptStorage);
    }

    @Test
    void createsAndPersistsABooking() {
        var travelerId = UUID.randomUUID().toString();

        var bookingId =
                service.create(
                        new CreateBookingCommand(
                                travelerId,
                                "FLIGHT",
                                UUID.randomUUID().toString(),
                                2,
                                new BigDecimal("450.00"),
                                "EUR"));

        assertThat(bookingId).isNotNull();
        verify(bookingRepository).save(any(Booking.class));
        verify(receiptStorage).store(any(Booking.class));
    }
}

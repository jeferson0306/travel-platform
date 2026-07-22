package com.travelplatform.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.booking.application.port.in.ConfirmBookingUseCase.ConfirmBookingCommand;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingId;
import com.travelplatform.booking.domain.booking.BookingNotFoundException;
import com.travelplatform.booking.domain.booking.BookingReference;
import com.travelplatform.booking.domain.booking.Email;
import com.travelplatform.booking.domain.booking.ItemType;
import com.travelplatform.booking.domain.booking.Money;
import com.travelplatform.booking.domain.booking.TravelerId;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmBookingService")
class ConfirmBookingServiceTest {

    @Mock BookingRepository bookingRepository;

    ConfirmBookingService service;

    @BeforeEach
    void setUp() {
        service = new ConfirmBookingService(bookingRepository);
    }

    @Test
    @DisplayName("confirms an existing pending booking")
    void confirmsAnExistingBooking() {
        var booking =
                Booking.create(
                        new TravelerId(UUID.randomUUID()),
                        new Email("traveler@example.com"),
                        new BookingReference(ItemType.FLIGHT, UUID.randomUUID().toString(), 1),
                        new Money(new BigDecimal("450.00"), "EUR"));
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        service.confirm(new ConfirmBookingCommand(booking.id().value().toString()));

        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("rejects confirming a booking that does not exist")
    void rejectsConfirmingAnUnknownBooking() {
        var id = UUID.randomUUID().toString();
        when(bookingRepository.findById(BookingId.of(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(new ConfirmBookingCommand(id)))
                .isInstanceOf(BookingNotFoundException.class);
    }
}

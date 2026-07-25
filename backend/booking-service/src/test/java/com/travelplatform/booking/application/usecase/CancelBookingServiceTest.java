package com.travelplatform.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.booking.application.port.in.CancelBookingUseCase.CancelBookingCommand;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingId;
import com.travelplatform.booking.domain.booking.BookingNotFoundException;
import com.travelplatform.booking.domain.booking.BookingReference;
import com.travelplatform.booking.domain.booking.Email;
import com.travelplatform.booking.domain.booking.ItemType;
import com.travelplatform.booking.domain.booking.Money;
import com.travelplatform.booking.domain.booking.TravelerId;
import io.quarkus.security.ForbiddenException;
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
@DisplayName("CancelBookingService")
class CancelBookingServiceTest {

    @Mock BookingRepository bookingRepository;

    CancelBookingService service;

    @BeforeEach
    void setUp() {
        service = new CancelBookingService(bookingRepository);
    }

    @Test
    @DisplayName("cancels an existing booking when the caller owns it")
    void cancelsAnExistingBooking() {
        var travelerId = new TravelerId(UUID.randomUUID());
        var booking =
                Booking.create(
                        travelerId,
                        new Email("traveler@example.com"),
                        new BookingReference(ItemType.HOTEL, UUID.randomUUID().toString(), 1, null),
                        new Money(new BigDecimal("95.00"), "EUR"));
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        service.cancel(
                new CancelBookingCommand(
                        booking.id().value().toString(), travelerId.value().toString()));

        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("rejects cancelling a booking that does not exist")
    void rejectsCancellingAnUnknownBooking() {
        var id = UUID.randomUUID().toString();
        when(bookingRepository.findById(BookingId.of(id))).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.cancel(
                                        new CancelBookingCommand(id, UUID.randomUUID().toString())))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("rejects cancelling a booking the caller does not own")
    void rejectsCancellingSomeoneElsesBooking() {
        var booking =
                Booking.create(
                        new TravelerId(UUID.randomUUID()),
                        new Email("traveler@example.com"),
                        new BookingReference(ItemType.HOTEL, UUID.randomUUID().toString(), 1, null),
                        new Money(new BigDecimal("95.00"), "EUR"));
        when(bookingRepository.findById(booking.id())).thenReturn(Optional.of(booking));

        var intruderId = UUID.randomUUID().toString();

        assertThatThrownBy(
                        () ->
                                service.cancel(
                                        new CancelBookingCommand(
                                                booking.id().value().toString(), intruderId)))
                .isInstanceOf(ForbiddenException.class);
    }
}

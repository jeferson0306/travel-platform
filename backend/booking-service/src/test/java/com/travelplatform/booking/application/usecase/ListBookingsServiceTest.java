package com.travelplatform.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.travelplatform.booking.application.port.in.ListBookingsUseCase.ListBookingsQuery;
import com.travelplatform.booking.application.port.out.BookingRepository;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingReference;
import com.travelplatform.booking.domain.booking.Email;
import com.travelplatform.booking.domain.booking.ItemType;
import com.travelplatform.booking.domain.booking.Money;
import com.travelplatform.booking.domain.booking.TravelerId;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListBookingsService")
class ListBookingsServiceTest {

    @Mock BookingRepository bookingRepository;

    ListBookingsService service;

    @BeforeEach
    void setUp() {
        service = new ListBookingsService(bookingRepository);
    }

    @Test
    void returnsTheRepositorysBookingsForTheGivenTraveler() {
        var travelerId = TravelerId.of(UUID.randomUUID().toString());
        var booking =
                Booking.create(
                        travelerId,
                        new Email("traveler@example.com"),
                        new BookingReference(
                                ItemType.FLIGHT, UUID.randomUUID().toString(), 1, null),
                        new Money(new BigDecimal("120.00"), "EUR"));
        when(bookingRepository.findByTravelerId(travelerId)).thenReturn(List.of(booking));

        var result = service.list(new ListBookingsQuery(travelerId.value().toString()));

        assertThat(result).containsExactly(booking);
    }
}

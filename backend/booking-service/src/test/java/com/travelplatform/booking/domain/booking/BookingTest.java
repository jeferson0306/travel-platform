package com.travelplatform.booking.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    void createRaisesBookingCreatedAndStartsPending() {
        var travelerId = new TravelerId(UUID.randomUUID());
        var reference = new BookingReference("flight-LIS-GRU-2026-08-01");

        var booking = Booking.create(travelerId, reference);

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.travelerId()).isEqualTo(travelerId);
        assertThat(booking.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(
                        BookingCreated.class,
                        event -> {
                            assertThat(event.bookingId()).isEqualTo(booking.id());
                            assertThat(event.travelerId()).isEqualTo(travelerId);
                            assertThat(event.reference()).isEqualTo(reference);
                        });
    }

    @Test
    void cancelRaisesBookingCancelledAndChangesStatus() {
        var booking =
                Booking.create(
                        new TravelerId(UUID.randomUUID()), new BookingReference("hotel-lisbon"));
        booking.pullDomainEvents();

        booking.cancel();

        assertThat(booking.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(
                        BookingCancelled.class,
                        event -> assertThat(event.bookingId()).isEqualTo(booking.id()));
    }

    @Test
    void cancelTwiceThrows() {
        var booking =
                Booking.create(
                        new TravelerId(UUID.randomUUID()), new BookingReference("hotel-lisbon"));
        booking.cancel();

        assertThatThrownBy(booking::cancel).isInstanceOf(BookingAlreadyCancelledException.class);
    }

    @Test
    void reconstitutedBookingRaisesNoDomainEvents() {
        var booking =
                Booking.reconstitute(
                        BookingId.newId(),
                        new TravelerId(UUID.randomUUID()),
                        new BookingReference("hotel-lisbon"),
                        BookingStatus.PENDING,
                        Instant.now());

        assertThat(booking.pullDomainEvents()).isEmpty();
    }
}

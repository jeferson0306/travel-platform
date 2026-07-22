package com.travelplatform.booking.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Booking")
class BookingTest {

    private static final Money AMOUNT = new Money(new BigDecimal("450.00"), "EUR");

    @Test
    void createRaisesBookingCreatedAndStartsPending() {
        var travelerId = new TravelerId(UUID.randomUUID());
        var reference = new BookingReference(ItemType.FLIGHT, UUID.randomUUID().toString(), 2);

        var booking = Booking.create(travelerId, reference, AMOUNT);

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.travelerId()).isEqualTo(travelerId);
        assertThat(booking.amount()).isEqualTo(AMOUNT);
        assertThat(booking.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(
                        BookingCreated.class,
                        event -> {
                            assertThat(event.bookingId()).isEqualTo(booking.id());
                            assertThat(event.travelerId()).isEqualTo(travelerId);
                            assertThat(event.reference()).isEqualTo(reference);
                            assertThat(event.amount()).isEqualTo(AMOUNT);
                        });
    }

    @Test
    void cancelRaisesBookingCancelledAndChangesStatus() {
        var booking =
                Booking.create(
                        new TravelerId(UUID.randomUUID()),
                        new BookingReference(ItemType.HOTEL, UUID.randomUUID().toString(), 1),
                        AMOUNT);
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
                        new TravelerId(UUID.randomUUID()),
                        new BookingReference(ItemType.HOTEL, UUID.randomUUID().toString(), 1),
                        AMOUNT);
        booking.cancel();

        assertThatThrownBy(booking::cancel).isInstanceOf(BookingAlreadyCancelledException.class);
    }

    @Test
    void confirmChangesStatusToConfirmed() {
        var booking =
                Booking.create(
                        new TravelerId(UUID.randomUUID()),
                        new BookingReference(ItemType.HOTEL, UUID.randomUUID().toString(), 1),
                        AMOUNT);

        booking.confirm();

        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirmTwiceThrows() {
        var booking =
                Booking.create(
                        new TravelerId(UUID.randomUUID()),
                        new BookingReference(ItemType.HOTEL, UUID.randomUUID().toString(), 1),
                        AMOUNT);
        booking.confirm();

        assertThatThrownBy(booking::confirm).isInstanceOf(BookingAlreadyConfirmedException.class);
    }

    @Test
    void reconstitutedBookingRaisesNoDomainEvents() {
        var booking =
                Booking.reconstitute(
                        BookingId.newId(),
                        new TravelerId(UUID.randomUUID()),
                        new BookingReference(ItemType.HOTEL, UUID.randomUUID().toString(), 1),
                        AMOUNT,
                        BookingStatus.PENDING,
                        Instant.now());

        assertThat(booking.pullDomainEvents()).isEmpty();
    }
}

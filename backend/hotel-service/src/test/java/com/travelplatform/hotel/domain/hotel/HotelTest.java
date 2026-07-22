package com.travelplatform.hotel.domain.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class HotelTest {

    @Test
    void createsAHotel() {
        var hotel =
                Hotel.create(
                        new HotelName("Lisbon Central"),
                        new City("Lisbon"),
                        new Money(new BigDecimal("120.00"), "EUR"),
                        20);

        assertThat(hotel.name().value()).isEqualTo("Lisbon Central");
        assertThat(hotel.city().value()).isEqualTo("Lisbon");
        assertThat(hotel.availableRooms()).isEqualTo(20);
    }

    @Test
    void rejectsNegativeAvailableRooms() {
        assertThatThrownBy(
                        () ->
                                Hotel.create(
                                        new HotelName("Lisbon Central"),
                                        new City("Lisbon"),
                                        new Money(new BigDecimal("120.00"), "EUR"),
                                        -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), "EUR"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

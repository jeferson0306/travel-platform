package com.travelplatform.hotel.domain.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Hotel")
class HotelTest {

    @Test
    void createsAHotel() {
        var hotel =
                Hotel.create(
                        new HotelName("Lisbon Central"),
                        new City("Lisbon"),
                        new Money(new BigDecimal("120.00"), "EUR"),
                        20,
                        "Rua Augusta 100",
                        4,
                        List.of("Free WiFi", "Pool"),
                        "A central hotel in Lisbon.",
                        4.3,
                        128);

        assertThat(hotel.name().value()).isEqualTo("Lisbon Central");
        assertThat(hotel.city().value()).isEqualTo("Lisbon");
        assertThat(hotel.availableRooms()).isEqualTo(20);
        assertThat(hotel.address()).isEqualTo("Rua Augusta 100");
        assertThat(hotel.starRating()).isEqualTo(4);
        assertThat(hotel.amenities()).containsExactly("Free WiFi", "Pool");
        assertThat(hotel.description()).isEqualTo("A central hotel in Lisbon.");
        assertThat(hotel.reviewScore()).isEqualTo(4.3);
        assertThat(hotel.reviewCount()).isEqualTo(128);
    }

    @Test
    void rejectsNegativeAvailableRooms() {
        assertThatThrownBy(
                        () ->
                                Hotel.create(
                                        new HotelName("Lisbon Central"),
                                        new City("Lisbon"),
                                        new Money(new BigDecimal("120.00"), "EUR"),
                                        -1,
                                        null,
                                        3,
                                        List.of(),
                                        null,
                                        null,
                                        0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), "EUR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsStarRatingBelowOne() {
        assertThatThrownBy(
                        () ->
                                Hotel.create(
                                        new HotelName("Lisbon Central"),
                                        new City("Lisbon"),
                                        new Money(new BigDecimal("120.00"), "EUR"),
                                        20,
                                        null,
                                        0,
                                        List.of(),
                                        null,
                                        null,
                                        0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsStarRatingAboveFive() {
        assertThatThrownBy(
                        () ->
                                Hotel.create(
                                        new HotelName("Lisbon Central"),
                                        new City("Lisbon"),
                                        new Money(new BigDecimal("120.00"), "EUR"),
                                        20,
                                        null,
                                        6,
                                        List.of(),
                                        null,
                                        null,
                                        0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsAmenitiesToEmptyListWhenOmitted() {
        var hotel =
                Hotel.create(
                        new HotelName("Lisbon Central"),
                        new City("Lisbon"),
                        new Money(new BigDecimal("120.00"), "EUR"),
                        20,
                        null,
                        3,
                        null,
                        null,
                        null,
                        0);

        assertThat(hotel.amenities()).isNotNull().isEmpty();
    }
}

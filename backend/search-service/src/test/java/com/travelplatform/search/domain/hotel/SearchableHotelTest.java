package com.travelplatform.search.domain.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SearchableHotel")
class SearchableHotelTest {

    @Test
    void constructsWithAllFieldsPopulated() {
        var hotelId = UUID.randomUUID().toString();

        var hotel =
                new SearchableHotel(
                        hotelId, "Grand Hotel", "Lisbon", new BigDecimal("120.00"), "EUR", 30);

        assertThat(hotel.hotelId()).isEqualTo(hotelId);
        assertThat(hotel.name()).isEqualTo("Grand Hotel");
        assertThat(hotel.city()).isEqualTo("Lisbon");
        assertThat(hotel.availableRooms()).isEqualTo(30);
    }

    @Test
    void rejectsANullName() {
        assertThatThrownBy(
                        () ->
                                new SearchableHotel(
                                        UUID.randomUUID().toString(),
                                        null,
                                        "Lisbon",
                                        new BigDecimal("120.00"),
                                        "EUR",
                                        30))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullCity() {
        assertThatThrownBy(
                        () ->
                                new SearchableHotel(
                                        UUID.randomUUID().toString(),
                                        "Grand Hotel",
                                        null,
                                        new BigDecimal("120.00"),
                                        "EUR",
                                        30))
                .isInstanceOf(NullPointerException.class);
    }
}

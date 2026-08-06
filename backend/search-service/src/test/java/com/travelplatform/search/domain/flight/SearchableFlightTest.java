package com.travelplatform.search.domain.flight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SearchableFlight")
class SearchableFlightTest {

    @Test
    void constructsWithAllFieldsPopulated() {
        var flightId = UUID.randomUUID().toString();

        var flight =
                new SearchableFlight(
                        flightId,
                        "LIS",
                        "GRU",
                        Instant.now(),
                        Instant.now().plusSeconds(36000),
                        new BigDecimal("450.00"),
                        "EUR",
                        150);

        assertThat(flight.flightId()).isEqualTo(flightId);
        assertThat(flight.origin()).isEqualTo("LIS");
        assertThat(flight.destination()).isEqualTo("GRU");
        assertThat(flight.availableSeats()).isEqualTo(150);
    }

    @Test
    void rejectsANullFlightId() {
        assertThatThrownBy(
                        () ->
                                new SearchableFlight(
                                        null,
                                        "LIS",
                                        "GRU",
                                        Instant.now(),
                                        Instant.now(),
                                        new BigDecimal("450.00"),
                                        "EUR",
                                        150))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullPriceAmount() {
        assertThatThrownBy(
                        () ->
                                new SearchableFlight(
                                        UUID.randomUUID().toString(),
                                        "LIS",
                                        "GRU",
                                        Instant.now(),
                                        Instant.now(),
                                        null,
                                        "EUR",
                                        150))
                .isInstanceOf(NullPointerException.class);
    }
}

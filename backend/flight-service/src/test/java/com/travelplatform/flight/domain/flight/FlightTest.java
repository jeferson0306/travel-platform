package com.travelplatform.flight.domain.flight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Flight")
class FlightTest {

    private static final AirportCode LIS = new AirportCode("LIS");
    private static final AirportCode GRU = new AirportCode("GRU");
    private static final Money PRICE = new Money(new BigDecimal("450.00"), "EUR");

    @Test
    void createsAFlightWithAFutureSchedule() {
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        var arrival = departure.plus(10, ChronoUnit.HOURS);

        var flight =
                Flight.create(
                        LIS,
                        GRU,
                        departure,
                        arrival,
                        PRICE,
                        120,
                        "TAP Air Portugal",
                        "TP",
                        "TP123",
                        "ECONOMY",
                        0);

        assertThat(flight.origin()).isEqualTo(LIS);
        assertThat(flight.destination()).isEqualTo(GRU);
        assertThat(flight.availableSeats()).isEqualTo(120);
        assertThat(flight.airline()).isEqualTo("TAP Air Portugal");
        assertThat(flight.airlineCode()).isEqualTo("TP");
        assertThat(flight.flightNumber()).isEqualTo("TP123");
        assertThat(flight.cabinClass()).isEqualTo("ECONOMY");
        assertThat(flight.stops()).isEqualTo(0);
    }

    @Test
    void createsAFlightWithoutCabinClass() {
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        var arrival = departure.plus(10, ChronoUnit.HOURS);

        var flight =
                Flight.create(
                        LIS,
                        GRU,
                        departure,
                        arrival,
                        PRICE,
                        120,
                        "TAP Air Portugal",
                        "TP",
                        "TP123",
                        null,
                        0);

        assertThat(flight.cabinClass()).isNull();
    }

    @Test
    void rejectsArrivalBeforeDeparture() {
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        var arrival = departure.minus(1, ChronoUnit.HOURS);

        assertThatThrownBy(
                        () ->
                                Flight.create(
                                        LIS,
                                        GRU,
                                        departure,
                                        arrival,
                                        PRICE,
                                        120,
                                        "TAP Air Portugal",
                                        "TP",
                                        "TP123",
                                        "ECONOMY",
                                        0))
                .isInstanceOf(InvalidFlightScheduleException.class);
    }

    @Test
    void rejectsNegativeAvailableSeats() {
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        var arrival = departure.plus(10, ChronoUnit.HOURS);

        assertThatThrownBy(
                        () ->
                                Flight.create(
                                        LIS,
                                        GRU,
                                        departure,
                                        arrival,
                                        PRICE,
                                        -1,
                                        "TAP Air Portugal",
                                        "TP",
                                        "TP123",
                                        "ECONOMY",
                                        0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeStops() {
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        var arrival = departure.plus(10, ChronoUnit.HOURS);

        assertThatThrownBy(
                        () ->
                                Flight.create(
                                        LIS,
                                        GRU,
                                        departure,
                                        arrival,
                                        PRICE,
                                        120,
                                        "TAP Air Portugal",
                                        "TP",
                                        "TP123",
                                        "ECONOMY",
                                        -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

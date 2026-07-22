package com.travelplatform.flight.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.travelplatform.flight.application.port.in.CreateFlightUseCase.CreateFlightCommand;
import com.travelplatform.flight.application.port.out.FlightRepository;
import com.travelplatform.flight.domain.flight.Flight;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateFlightService")
class CreateFlightServiceTest {

    @Mock FlightRepository flightRepository;

    CreateFlightService service;

    @BeforeEach
    void setUp() {
        service = new CreateFlightService(flightRepository);
    }

    @Test
    void createsAndPersistsAFlight() {
        var departure = Instant.now().plus(1, ChronoUnit.DAYS);
        var arrival = departure.plus(10, ChronoUnit.HOURS);

        var id =
                service.create(
                        new CreateFlightCommand(
                                "LIS",
                                "GRU",
                                departure,
                                arrival,
                                new BigDecimal("450.00"),
                                "EUR",
                                120));

        assertThat(id).isNotNull();
        verify(flightRepository).save(any(Flight.class));
    }
}

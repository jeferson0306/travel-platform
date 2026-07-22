package com.travelplatform.search.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.travelplatform.search.application.port.in.IndexFlightUseCase.IndexFlightCommand;
import com.travelplatform.search.application.port.out.FlightSearchRepository;
import com.travelplatform.search.domain.flight.SearchableFlight;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexFlightService")
class IndexFlightServiceTest {

    @Mock FlightSearchRepository flightSearchRepository;

    IndexFlightService service;

    @BeforeEach
    void setUp() {
        service = new IndexFlightService(flightSearchRepository);
    }

    @Test
    void indexesTheFlightTranslatedFromTheCommand() {
        var flightId = UUID.randomUUID().toString();
        var departureAt = Instant.now();
        var arrivalAt = departureAt.plusSeconds(36000);

        service.index(
                new IndexFlightCommand(
                        flightId,
                        "LIS",
                        "GRU",
                        departureAt,
                        arrivalAt,
                        new BigDecimal("450.00"),
                        "EUR",
                        150));

        var captor = ArgumentCaptor.forClass(SearchableFlight.class);
        verify(flightSearchRepository).index(captor.capture());
        assertThat(captor.getValue().flightId()).isEqualTo(flightId);
        assertThat(captor.getValue().origin()).isEqualTo("LIS");
        assertThat(captor.getValue().destination()).isEqualTo("GRU");
        assertThat(captor.getValue().availableSeats()).isEqualTo(150);
    }
}

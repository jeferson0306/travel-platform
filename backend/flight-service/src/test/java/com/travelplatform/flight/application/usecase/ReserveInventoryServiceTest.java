package com.travelplatform.flight.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.travelplatform.flight.application.port.in.ReserveInventoryUseCase.ReserveInventoryCommand;
import com.travelplatform.flight.application.port.out.FlightRepository;
import com.travelplatform.flight.domain.flight.FlightId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveInventoryServiceTest {

    @Mock FlightRepository flightRepository;

    ReserveInventoryService service;

    @BeforeEach
    void setUp() {
        service = new ReserveInventoryService(flightRepository);
    }

    @Test
    void delegatesToRepositoryAndReturnsItsResult() {
        var flightId = FlightId.newId();
        when(flightRepository.tryReserve(flightId, 2)).thenReturn(true);

        var reserved = service.reserve(new ReserveInventoryCommand(flightId.value().toString(), 2));

        assertThat(reserved).isTrue();
    }

    @Test
    void returnsFalseWhenRepositoryCannotReserve() {
        var flightId = FlightId.newId();
        when(flightRepository.tryReserve(flightId, 5)).thenReturn(false);

        var reserved = service.reserve(new ReserveInventoryCommand(flightId.value().toString(), 5));

        assertThat(reserved).isFalse();
    }

    @Test
    void unknownFlightIdStillReachesRepository() {
        var randomId = UUID.randomUUID().toString();

        service.reserve(new ReserveInventoryCommand(randomId, 1));

        org.mockito.Mockito.verify(flightRepository).tryReserve(FlightId.of(randomId), 1);
    }
}

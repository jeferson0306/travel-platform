package com.travelplatform.flight.application.usecase;

import com.travelplatform.flight.application.port.in.ReserveInventoryUseCase;
import com.travelplatform.flight.application.port.out.FlightRepository;
import com.travelplatform.flight.domain.flight.FlightId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReserveInventoryService implements ReserveInventoryUseCase {

    private final FlightRepository flightRepository;

    public ReserveInventoryService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    public boolean reserve(ReserveInventoryCommand command) {
        return flightRepository.tryReserve(FlightId.of(command.flightId()), command.quantity());
    }
}

package com.travelplatform.flight.application.port.in;

public interface ReserveInventoryUseCase {

    /** Returns false if the flight was not found or does not have enough available seats. */
    boolean reserve(ReserveInventoryCommand command);

    record ReserveInventoryCommand(String flightId, int quantity) {}
}

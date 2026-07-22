package com.travelplatform.hotel.application.port.in;

public interface ReserveInventoryUseCase {

    /** Returns false if the hotel was not found or does not have enough available rooms. */
    boolean reserve(ReserveInventoryCommand command);

    record ReserveInventoryCommand(String hotelId, int quantity) {}
}

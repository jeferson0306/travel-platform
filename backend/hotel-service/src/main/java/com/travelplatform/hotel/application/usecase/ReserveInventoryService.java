package com.travelplatform.hotel.application.usecase;

import com.travelplatform.hotel.application.port.in.ReserveInventoryUseCase;
import com.travelplatform.hotel.application.port.out.HotelRepository;
import com.travelplatform.hotel.domain.hotel.HotelId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReserveInventoryService implements ReserveInventoryUseCase {

    private final HotelRepository hotelRepository;

    public ReserveInventoryService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public boolean reserve(ReserveInventoryCommand command) {
        return hotelRepository.tryReserve(HotelId.of(command.hotelId()), command.quantity());
    }
}

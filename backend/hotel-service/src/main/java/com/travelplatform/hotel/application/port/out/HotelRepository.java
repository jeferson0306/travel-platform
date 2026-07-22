package com.travelplatform.hotel.application.port.out;

import com.travelplatform.hotel.domain.hotel.City;
import com.travelplatform.hotel.domain.hotel.Hotel;
import com.travelplatform.hotel.domain.hotel.HotelId;
import java.util.List;
import java.util.Optional;

public interface HotelRepository {

    void save(Hotel hotel);

    Optional<Hotel> findById(HotelId id);

    List<Hotel> search(City city);

    /**
     * Atomically decrements {@code availableRooms} by {@code quantity} if and only if enough rooms
     * are available. Returns false (no-op) if the hotel does not exist or has insufficient rooms -
     * the caller decides what that means (retry, dead-letter, ...).
     */
    boolean tryReserve(HotelId id, int quantity);
}

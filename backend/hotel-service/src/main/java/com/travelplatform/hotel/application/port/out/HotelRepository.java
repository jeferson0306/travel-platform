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
}

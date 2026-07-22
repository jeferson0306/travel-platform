package com.travelplatform.hotel.application.usecase;

import com.travelplatform.hotel.application.port.in.SearchHotelsUseCase;
import com.travelplatform.hotel.application.port.out.HotelRepository;
import com.travelplatform.hotel.domain.hotel.City;
import com.travelplatform.hotel.domain.hotel.Hotel;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class SearchHotelsService implements SearchHotelsUseCase {

    private final HotelRepository hotelRepository;

    public SearchHotelsService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public List<Hotel> search(SearchHotelsQuery query) {
        return hotelRepository.search(new City(query.city()));
    }
}

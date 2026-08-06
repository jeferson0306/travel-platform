package com.travelplatform.search.application.usecase;

import com.travelplatform.search.application.port.in.IndexHotelUseCase;
import com.travelplatform.search.application.port.out.HotelSearchRepository;
import com.travelplatform.search.domain.hotel.SearchableHotel;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IndexHotelService implements IndexHotelUseCase {

    private final HotelSearchRepository hotelSearchRepository;

    public IndexHotelService(HotelSearchRepository hotelSearchRepository) {
        this.hotelSearchRepository = hotelSearchRepository;
    }

    @Override
    public void index(IndexHotelCommand command) {
        hotelSearchRepository.index(
                new SearchableHotel(
                        command.hotelId(),
                        command.name(),
                        command.city(),
                        command.pricePerNightAmount(),
                        command.pricePerNightCurrency(),
                        command.availableRooms()));
    }
}

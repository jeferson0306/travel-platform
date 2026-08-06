package com.travelplatform.search.application.usecase;

import com.travelplatform.search.application.port.in.SearchHotelsUseCase;
import com.travelplatform.search.application.port.out.HotelSearchRepository;
import com.travelplatform.search.domain.hotel.SearchableHotel;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class SearchHotelsService implements SearchHotelsUseCase {

    private final HotelSearchRepository hotelSearchRepository;

    public SearchHotelsService(HotelSearchRepository hotelSearchRepository) {
        this.hotelSearchRepository = hotelSearchRepository;
    }

    @Override
    public List<SearchableHotel> search(SearchHotelsQuery query) {
        if (query.prefix() != null && !query.prefix().isBlank()) {
            return hotelSearchRepository.autocomplete(query.prefix());
        }
        return hotelSearchRepository.searchByCity(query.city());
    }
}

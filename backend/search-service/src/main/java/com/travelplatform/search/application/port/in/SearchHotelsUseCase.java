package com.travelplatform.search.application.port.in;

import com.travelplatform.search.domain.hotel.SearchableHotel;
import java.util.List;

public interface SearchHotelsUseCase {

    /**
     * Either {@code prefix} (autocomplete) or {@code city} (exact-city search) is expected to be
     * set - see {@link com.travelplatform.search.application.usecase.SearchHotelsService}.
     */
    List<SearchableHotel> search(SearchHotelsQuery query);

    record SearchHotelsQuery(String city, String prefix) {}
}

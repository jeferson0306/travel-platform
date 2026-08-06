package com.travelplatform.search.application.port.out;

import com.travelplatform.search.domain.hotel.SearchableHotel;
import java.util.List;

public interface HotelSearchRepository {

    /** Upsert by {@code hotelId} - reindexing an already-indexed hotel is a no-op overwrite. */
    void index(SearchableHotel hotel);

    List<SearchableHotel> searchByCity(String city);

    /** Prefix match against hotel name and city. */
    List<SearchableHotel> autocomplete(String prefix);
}

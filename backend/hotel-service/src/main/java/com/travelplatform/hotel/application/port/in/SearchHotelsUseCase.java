package com.travelplatform.hotel.application.port.in;

import com.travelplatform.hotel.domain.hotel.Hotel;
import java.util.List;

public interface SearchHotelsUseCase {

    List<Hotel> search(SearchHotelsQuery query);

    record SearchHotelsQuery(String city) {}
}

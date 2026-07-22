package com.travelplatform.hotel.domain.hotel;

import com.travelplatform.hotel.domain.shared.DomainException;

public final class HotelNotFoundException extends DomainException {

    public HotelNotFoundException(HotelId id) {
        super("Hotel not found: " + id.value());
    }
}

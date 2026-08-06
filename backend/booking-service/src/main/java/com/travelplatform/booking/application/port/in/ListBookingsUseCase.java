package com.travelplatform.booking.application.port.in;

import com.travelplatform.booking.domain.booking.Booking;
import java.util.List;

public interface ListBookingsUseCase {

    List<Booking> list(ListBookingsQuery query);

    record ListBookingsQuery(String travelerId) {}
}

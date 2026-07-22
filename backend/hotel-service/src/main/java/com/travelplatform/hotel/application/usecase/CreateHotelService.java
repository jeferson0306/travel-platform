package com.travelplatform.hotel.application.usecase;

import com.travelplatform.hotel.application.port.in.CreateHotelUseCase;
import com.travelplatform.hotel.application.port.out.HotelRepository;
import com.travelplatform.hotel.domain.hotel.City;
import com.travelplatform.hotel.domain.hotel.Hotel;
import com.travelplatform.hotel.domain.hotel.HotelId;
import com.travelplatform.hotel.domain.hotel.HotelName;
import com.travelplatform.hotel.domain.hotel.Money;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CreateHotelService implements CreateHotelUseCase {

    private final HotelRepository hotelRepository;

    public CreateHotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public HotelId create(CreateHotelCommand command) {
        var hotel =
                Hotel.create(
                        new HotelName(command.name()),
                        new City(command.city()),
                        new Money(command.pricePerNightAmount(), command.pricePerNightCurrency()),
                        command.availableRooms());
        hotelRepository.save(hotel);
        return hotel.id();
    }
}

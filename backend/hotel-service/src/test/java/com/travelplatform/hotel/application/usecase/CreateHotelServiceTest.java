package com.travelplatform.hotel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.travelplatform.hotel.application.port.in.CreateHotelUseCase.CreateHotelCommand;
import com.travelplatform.hotel.application.port.out.HotelRepository;
import com.travelplatform.hotel.domain.hotel.Hotel;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateHotelService")
class CreateHotelServiceTest {

    @Mock HotelRepository hotelRepository;

    CreateHotelService service;

    @BeforeEach
    void setUp() {
        service = new CreateHotelService(hotelRepository);
    }

    @Test
    void createsAndPersistsAHotel() {
        var id =
                service.create(
                        new CreateHotelCommand(
                                "Lisbon Central", "Lisbon", new BigDecimal("120.00"), "EUR", 20));

        assertThat(id).isNotNull();
        verify(hotelRepository).save(any(Hotel.class));
    }
}

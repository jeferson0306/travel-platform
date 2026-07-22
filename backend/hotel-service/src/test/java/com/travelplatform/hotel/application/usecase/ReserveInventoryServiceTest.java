package com.travelplatform.hotel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.travelplatform.hotel.application.port.in.ReserveInventoryUseCase.ReserveInventoryCommand;
import com.travelplatform.hotel.application.port.out.HotelRepository;
import com.travelplatform.hotel.domain.hotel.HotelId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveInventoryServiceTest {

    @Mock HotelRepository hotelRepository;

    ReserveInventoryService service;

    @BeforeEach
    void setUp() {
        service = new ReserveInventoryService(hotelRepository);
    }

    @Test
    void delegatesToRepositoryAndReturnsItsResult() {
        var hotelId = HotelId.newId();
        when(hotelRepository.tryReserve(hotelId, 2)).thenReturn(true);

        var reserved = service.reserve(new ReserveInventoryCommand(hotelId.value().toString(), 2));

        assertThat(reserved).isTrue();
    }

    @Test
    void returnsFalseWhenRepositoryCannotReserve() {
        var hotelId = HotelId.newId();
        when(hotelRepository.tryReserve(hotelId, 5)).thenReturn(false);

        var reserved = service.reserve(new ReserveInventoryCommand(hotelId.value().toString(), 5));

        assertThat(reserved).isFalse();
    }

    @Test
    void unknownHotelIdStillReachesRepository() {
        var randomId = UUID.randomUUID().toString();

        service.reserve(new ReserveInventoryCommand(randomId, 1));

        org.mockito.Mockito.verify(hotelRepository).tryReserve(HotelId.of(randomId), 1);
    }
}

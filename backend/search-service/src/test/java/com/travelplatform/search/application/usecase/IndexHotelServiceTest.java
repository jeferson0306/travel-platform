package com.travelplatform.search.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.travelplatform.search.application.port.in.IndexHotelUseCase.IndexHotelCommand;
import com.travelplatform.search.application.port.out.HotelSearchRepository;
import com.travelplatform.search.domain.hotel.SearchableHotel;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexHotelService")
class IndexHotelServiceTest {

    @Mock HotelSearchRepository hotelSearchRepository;

    IndexHotelService service;

    @BeforeEach
    void setUp() {
        service = new IndexHotelService(hotelSearchRepository);
    }

    @Test
    void indexesTheHotelTranslatedFromTheCommand() {
        var hotelId = UUID.randomUUID().toString();

        service.index(
                new IndexHotelCommand(
                        hotelId, "Grand Hotel", "Lisbon", new BigDecimal("120.00"), "EUR", 30));

        var captor = ArgumentCaptor.forClass(SearchableHotel.class);
        verify(hotelSearchRepository).index(captor.capture());
        assertThat(captor.getValue().hotelId()).isEqualTo(hotelId);
        assertThat(captor.getValue().name()).isEqualTo("Grand Hotel");
        assertThat(captor.getValue().city()).isEqualTo("Lisbon");
        assertThat(captor.getValue().availableRooms()).isEqualTo(30);
    }
}

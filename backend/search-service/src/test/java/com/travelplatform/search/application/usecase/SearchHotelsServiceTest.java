package com.travelplatform.search.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.search.application.port.in.SearchHotelsUseCase.SearchHotelsQuery;
import com.travelplatform.search.application.port.out.HotelSearchRepository;
import com.travelplatform.search.domain.hotel.SearchableHotel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchHotelsService")
class SearchHotelsServiceTest {

    @Mock HotelSearchRepository hotelSearchRepository;

    SearchHotelsService service;

    @BeforeEach
    void setUp() {
        service = new SearchHotelsService(hotelSearchRepository);
    }

    private SearchableHotel aHotel() {
        return new SearchableHotel(
                UUID.randomUUID().toString(),
                "Grand Hotel",
                "Lisbon",
                new BigDecimal("120.00"),
                "EUR",
                30);
    }

    @Test
    void aQueryWithAPrefixDelegatesToAutocomplete() {
        when(hotelSearchRepository.autocomplete("gra")).thenReturn(List.of(aHotel()));

        var result = service.search(new SearchHotelsQuery(null, "gra"));

        assertThat(result).hasSize(1);
        verify(hotelSearchRepository, never()).searchByCity(anyString());
    }

    @Test
    void aQueryWithoutAPrefixDelegatesToCitySearch() {
        when(hotelSearchRepository.searchByCity("Lisbon")).thenReturn(List.of(aHotel()));

        var result = service.search(new SearchHotelsQuery("Lisbon", null));

        assertThat(result).hasSize(1);
        verify(hotelSearchRepository, never()).autocomplete(anyString());
    }
}

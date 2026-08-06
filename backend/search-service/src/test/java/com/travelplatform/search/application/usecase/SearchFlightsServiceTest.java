package com.travelplatform.search.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.search.application.port.in.SearchFlightsUseCase.SearchFlightsQuery;
import com.travelplatform.search.application.port.out.FlightSearchRepository;
import com.travelplatform.search.domain.flight.SearchableFlight;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchFlightsService")
class SearchFlightsServiceTest {

    @Mock FlightSearchRepository flightSearchRepository;

    SearchFlightsService service;

    @BeforeEach
    void setUp() {
        service = new SearchFlightsService(flightSearchRepository);
    }

    private SearchableFlight aFlight() {
        return new SearchableFlight(
                UUID.randomUUID().toString(),
                "LIS",
                "GRU",
                Instant.now(),
                Instant.now().plusSeconds(36000),
                new BigDecimal("450.00"),
                "EUR",
                150);
    }

    @Test
    void aQueryWithAPrefixDelegatesToAutocomplete() {
        when(flightSearchRepository.autocomplete("LI")).thenReturn(List.of(aFlight()));

        var result = service.search(new SearchFlightsQuery(null, null, "LI"));

        assertThat(result).hasSize(1);
        verify(flightSearchRepository, never()).searchByRoute(anyString(), anyString());
    }

    @Test
    void aQueryWithoutAPrefixDelegatesToRouteSearch() {
        when(flightSearchRepository.searchByRoute("LIS", "GRU")).thenReturn(List.of(aFlight()));

        var result = service.search(new SearchFlightsQuery("LIS", "GRU", null));

        assertThat(result).hasSize(1);
        verify(flightSearchRepository, never()).autocomplete(anyString());
    }
}

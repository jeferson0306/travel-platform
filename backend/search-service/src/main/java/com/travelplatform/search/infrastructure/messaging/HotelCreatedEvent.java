package com.travelplatform.search.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * The subset of hotel-service's {@code HotelCreated} domain event this service cares about - see
 * docs/events/hotel-events.md.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HotelCreatedEvent(
        HotelIdDto hotelId,
        HotelNameDto name,
        CityDto city,
        MoneyDto pricePerNight,
        int availableRooms) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HotelIdDto(String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HotelNameDto(String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CityDto(String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MoneyDto(BigDecimal amount, String currency) {}
}

package com.travelplatform.hotel.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The subset of booking-service's {@code BookingCreated} domain event this service cares about -
 * see docs/events/booking-events.md. {@code @JsonIgnoreProperties} tolerates fields (travelerId,
 * occurredOn) this consumer has no use for.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingCreatedEvent(BookingIdDto bookingId, ReferenceDto reference) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingIdDto(String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReferenceDto(String itemType, String itemId, int quantity) {}
}

package com.travelplatform.notification.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The subset of booking-service's {@code BookingConfirmed} domain event this service cares about -
 * see docs/events/booking-events.md. {@code @JsonIgnoreProperties} tolerates fields (travelerId,
 * occurredOn) this consumer has no use for.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingConfirmedEvent(BookingIdDto bookingId, TravelerEmailDto travelerEmail) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingIdDto(String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TravelerEmailDto(String value) {}
}

package com.travelplatform.payment.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The subset of booking-service's {@code BookingCancelled} domain event this service cares about.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingCancelledEvent(BookingIdDto bookingId) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingIdDto(String value) {}
}

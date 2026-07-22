package com.travelplatform.booking.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The subset of payment-service's {@code PaymentAuthorized}/{@code PaymentFailed} domain events
 * this service cares about - both carry a bookingId, which is all either consumer needs. See
 * docs/adr/0010-payment-saga.md.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentOutcomeEvent(BookingIdDto bookingId) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingIdDto(String value) {}
}

package com.travelplatform.payment.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * The subset of booking-service's {@code BookingCreated} domain event this service cares about -
 * see docs/events/booking-events.md. {@code @JsonIgnoreProperties} tolerates fields (travelerId,
 * reference, occurredOn) this consumer has no use for.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingCreatedEvent(BookingIdDto bookingId, AmountDto amount) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingIdDto(String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AmountDto(BigDecimal amount, String currency) {}
}

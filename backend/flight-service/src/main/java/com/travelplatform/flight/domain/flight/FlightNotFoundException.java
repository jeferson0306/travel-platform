package com.travelplatform.flight.domain.flight;

import com.travelplatform.flight.domain.shared.DomainException;

public final class FlightNotFoundException extends DomainException {

    public FlightNotFoundException(FlightId id) {
        super("Flight not found: " + id.value());
    }
}

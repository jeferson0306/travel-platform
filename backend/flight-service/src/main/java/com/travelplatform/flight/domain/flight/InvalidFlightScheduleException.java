package com.travelplatform.flight.domain.flight;

import com.travelplatform.flight.domain.shared.DomainException;

public final class InvalidFlightScheduleException extends DomainException {

    public InvalidFlightScheduleException() {
        super("arrivalAt must be after departureAt");
    }
}

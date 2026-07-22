package com.travelplatform.notification.domain.notification;

import com.travelplatform.notification.domain.shared.DomainException;

public final class NotificationNotFoundException extends DomainException {

    public NotificationNotFoundException(BookingId bookingId) {
        super("No notification found for booking: " + bookingId.value());
    }
}

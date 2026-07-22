package com.travelplatform.notification.application.port.out;

import com.travelplatform.notification.domain.notification.BookingId;
import com.travelplatform.notification.domain.notification.Notification;
import java.util.Optional;

public interface NotificationRepository {

    void save(Notification notification);

    Optional<Notification> findByBookingId(BookingId bookingId);
}

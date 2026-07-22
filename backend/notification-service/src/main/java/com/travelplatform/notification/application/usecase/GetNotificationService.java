package com.travelplatform.notification.application.usecase;

import com.travelplatform.notification.application.port.in.GetNotificationUseCase;
import com.travelplatform.notification.application.port.out.NotificationRepository;
import com.travelplatform.notification.domain.notification.BookingId;
import com.travelplatform.notification.domain.notification.Notification;
import com.travelplatform.notification.domain.notification.NotificationNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GetNotificationService implements GetNotificationUseCase {

    private final NotificationRepository notificationRepository;

    public GetNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification getByBookingId(GetNotificationQuery query) {
        var bookingId = BookingId.of(query.bookingId());
        return notificationRepository
                .findByBookingId(bookingId)
                .orElseThrow(() -> new NotificationNotFoundException(bookingId));
    }
}

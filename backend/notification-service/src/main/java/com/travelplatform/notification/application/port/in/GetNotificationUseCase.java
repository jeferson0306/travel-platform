package com.travelplatform.notification.application.port.in;

import com.travelplatform.notification.domain.notification.Notification;

public interface GetNotificationUseCase {

    Notification getByBookingId(GetNotificationQuery query);

    record GetNotificationQuery(String bookingId) {}
}

package com.travelplatform.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.travelplatform.notification.application.port.in.GetNotificationUseCase.GetNotificationQuery;
import com.travelplatform.notification.application.port.out.NotificationRepository;
import com.travelplatform.notification.domain.notification.BookingId;
import com.travelplatform.notification.domain.notification.Email;
import com.travelplatform.notification.domain.notification.Notification;
import com.travelplatform.notification.domain.notification.NotificationNotFoundException;
import com.travelplatform.notification.domain.notification.NotificationType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetNotificationService")
class GetNotificationServiceTest {

    @Mock NotificationRepository notificationRepository;

    GetNotificationService service;

    @BeforeEach
    void setUp() {
        service = new GetNotificationService(notificationRepository);
    }

    @Test
    void returnsTheNotificationWhenOneExistsForTheBooking() {
        var bookingId = new BookingId(UUID.randomUUID());
        var notification =
                Notification.sent(
                        bookingId,
                        new Email("traveler@example.com"),
                        NotificationType.BOOKING_CONFIRMATION);
        when(notificationRepository.findByBookingId(bookingId))
                .thenReturn(Optional.of(notification));

        var result = service.getByBookingId(new GetNotificationQuery(bookingId.value().toString()));

        assertThat(result).isEqualTo(notification);
    }

    @Test
    void throwsWhenNoNotificationExistsForTheBooking() {
        var bookingId = new BookingId(UUID.randomUUID());
        when(notificationRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.getByBookingId(
                                        new GetNotificationQuery(bookingId.value().toString())))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}

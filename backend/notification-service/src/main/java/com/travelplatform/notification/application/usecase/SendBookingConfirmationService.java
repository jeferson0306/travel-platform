package com.travelplatform.notification.application.usecase;

import com.travelplatform.notification.application.port.in.SendBookingConfirmationUseCase;
import com.travelplatform.notification.application.port.out.EmailGateway;
import com.travelplatform.notification.application.port.out.NotificationRepository;
import com.travelplatform.notification.domain.notification.BookingId;
import com.travelplatform.notification.domain.notification.Email;
import com.travelplatform.notification.domain.notification.Notification;
import com.travelplatform.notification.domain.notification.NotificationType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SendBookingConfirmationService implements SendBookingConfirmationUseCase {

    private final NotificationRepository notificationRepository;
    private final EmailGateway emailGateway;

    public SendBookingConfirmationService(
            NotificationRepository notificationRepository, EmailGateway emailGateway) {
        this.notificationRepository = notificationRepository;
        this.emailGateway = emailGateway;
    }

    @Override
    public void send(SendBookingConfirmationCommand command) {
        var bookingId = BookingId.of(command.bookingId());
        var recipient = new Email(command.recipientEmail());

        // If the gateway throws, this method throws too - no Notification is saved, so the
        // caller (BookingConfirmedConsumer) schedules a retry instead of silently losing it.
        emailGateway.send(
                recipient,
                "Your booking is confirmed",
                "Booking " + bookingId.value() + " is confirmed. Thank you for booking with us!");

        notificationRepository.save(
                Notification.sent(bookingId, recipient, NotificationType.BOOKING_CONFIRMATION));
    }
}

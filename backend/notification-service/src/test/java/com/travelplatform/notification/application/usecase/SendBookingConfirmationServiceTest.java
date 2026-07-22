package com.travelplatform.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.travelplatform.notification.application.port.in.SendBookingConfirmationUseCase.SendBookingConfirmationCommand;
import com.travelplatform.notification.application.port.out.EmailGateway;
import com.travelplatform.notification.application.port.out.NotificationRepository;
import com.travelplatform.notification.domain.notification.Email;
import com.travelplatform.notification.domain.notification.Notification;
import com.travelplatform.notification.domain.notification.NotificationStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendBookingConfirmationService")
class SendBookingConfirmationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock EmailGateway emailGateway;

    SendBookingConfirmationService service;

    @BeforeEach
    void setUp() {
        service = new SendBookingConfirmationService(notificationRepository, emailGateway);
    }

    @Test
    void sendsTheEmailAndSavesASentNotification() {
        var bookingId = UUID.randomUUID().toString();

        service.send(new SendBookingConfirmationCommand(bookingId, "traveler@example.com"));

        verify(emailGateway).send(eq(new Email("traveler@example.com")), anyString(), anyString());
    }

    @Test
    void savesTheNotificationOnlyAfterTheEmailIsSentSuccessfully() {
        var bookingId = UUID.randomUUID().toString();

        service.send(new SendBookingConfirmationCommand(bookingId, "traveler@example.com"));

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(NotificationStatus.SENT);
        assertThat(captor.getValue().bookingId().value().toString()).isEqualTo(bookingId);
        assertThat(captor.getValue().recipient()).isEqualTo(new Email("traveler@example.com"));
    }

    @Test
    void propagatesAGatewayFailureAndSavesNothing() {
        doThrow(new RuntimeException("gateway unavailable"))
                .when(emailGateway)
                .send(any(Email.class), anyString(), anyString());

        assertThatThrownBy(
                        () ->
                                service.send(
                                        new SendBookingConfirmationCommand(
                                                UUID.randomUUID().toString(),
                                                "traveler@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("gateway unavailable");

        verify(notificationRepository, never()).save(any(Notification.class));
    }
}

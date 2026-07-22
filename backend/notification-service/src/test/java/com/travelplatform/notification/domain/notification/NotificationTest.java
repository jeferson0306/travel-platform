package com.travelplatform.notification.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Notification")
class NotificationTest {

    private static final BookingId BOOKING_ID = new BookingId(UUID.randomUUID());
    private static final Email RECIPIENT = new Email("traveler@example.com");

    @Test
    void sentStartsInSentStatus() {
        var notification =
                Notification.sent(BOOKING_ID, RECIPIENT, NotificationType.BOOKING_CONFIRMATION);

        assertThat(notification.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(notification.recipient()).isEqualTo(RECIPIENT);
        assertThat(notification.type()).isEqualTo(NotificationType.BOOKING_CONFIRMATION);
        assertThat(notification.id()).isNotNull();
        assertThat(notification.createdAt()).isNotNull();
    }

    @Test
    void failedStartsInFailedStatus() {
        var notification =
                Notification.failed(BOOKING_ID, RECIPIENT, NotificationType.BOOKING_CONFIRMATION);

        assertThat(notification.status()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void reconstituteRebuildsExactlyWhatWasPersisted() {
        var id = NotificationId.newId();
        var createdAt = java.time.Instant.now();

        var notification =
                Notification.reconstitute(
                        id,
                        BOOKING_ID,
                        RECIPIENT,
                        NotificationType.BOOKING_CONFIRMATION,
                        NotificationStatus.SENT,
                        createdAt);

        assertThat(notification.id()).isEqualTo(id);
        assertThat(notification.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(notification.recipient()).isEqualTo(RECIPIENT);
        assertThat(notification.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.createdAt()).isEqualTo(createdAt);
    }
}

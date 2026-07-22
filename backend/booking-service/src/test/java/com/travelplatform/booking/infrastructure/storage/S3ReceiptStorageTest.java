package com.travelplatform.booking.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.booking.domain.booking.Booking;
import com.travelplatform.booking.domain.booking.BookingReference;
import com.travelplatform.booking.domain.booking.Email;
import com.travelplatform.booking.domain.booking.ItemType;
import com.travelplatform.booking.domain.booking.Money;
import com.travelplatform.booking.domain.booking.TravelerId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Unit-level (mocked S3Client), not a real-LocalStack integration test, because CI has no
 * LocalStack service - see docs/adr/0009-booking-receipts-in-s3.md. The best-effort behavior this
 * test asserts is exactly what makes that safe: a failed/unavailable S3 never fails booking
 * creation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3ReceiptStorage")
class S3ReceiptStorageTest {

    @Mock S3Client s3Client;

    S3ReceiptStorage storage;

    @BeforeEach
    void setUp() {
        // findAndRegisterModules() picks up JavaTimeModule (for Instant) - Quarkus's own
        // ObjectMapper bean does this automatically; this test builds a plain one, so it must too.
        storage =
                new S3ReceiptStorage(
                        s3Client, new ObjectMapper().findAndRegisterModules(), "booking-receipts");
    }

    @Test
    void writesTheReceiptToTheConfiguredBucketAndKey() {
        var booking = aBooking();

        storage.store(booking);

        var requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        var request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("booking-receipts");
        assertThat(request.key()).isEqualTo("receipts/" + booking.id().value() + ".json");
    }

    @Test
    void neverPropagatesAFailureAsBestEffort() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 unavailable"));

        assertThatCode(() -> storage.store(aBooking())).doesNotThrowAnyException();
    }

    private Booking aBooking() {
        return Booking.create(
                new TravelerId(UUID.randomUUID()),
                new Email("traveler@example.com"),
                new BookingReference(ItemType.FLIGHT, UUID.randomUUID().toString(), 2),
                new Money(new BigDecimal("450.00"), "EUR"));
    }
}

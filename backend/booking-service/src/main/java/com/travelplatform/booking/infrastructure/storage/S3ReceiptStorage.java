package com.travelplatform.booking.infrastructure.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplatform.booking.application.port.out.ReceiptStorage;
import com.travelplatform.booking.domain.booking.Booking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Writes the booking receipt to S3 (LocalStack locally - see
 * docs/adr/0008-use-localstack-and-terraform-for-aws-resources.md). Best-effort by design (ADR
 * 0009): any failure is logged, never propagated, so it can never fail booking creation.
 */
@ApplicationScoped
public class S3ReceiptStorage implements ReceiptStorage {

    private static final Logger LOG = Logger.getLogger(S3ReceiptStorage.class);

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final String bucket;

    public S3ReceiptStorage(
            S3Client s3Client,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "booking.aws.receipts-bucket", defaultValue = "booking-receipts")
                    String bucket) {
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.bucket = bucket;
    }

    @Override
    public void store(Booking booking) {
        var bookingId = booking.id().value().toString();
        try {
            var body = objectMapper.writeValueAsBytes(BookingReceipt.from(booking));
            var request =
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key("receipts/" + bookingId + ".json")
                            .contentType("application/json")
                            .build();
            s3Client.putObject(request, RequestBody.fromBytes(body));
        } catch (Exception e) {
            LOG.error("Failed to write receipt to S3 for booking " + bookingId, e);
        }
    }
}

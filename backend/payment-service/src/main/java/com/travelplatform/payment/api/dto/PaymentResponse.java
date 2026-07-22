package com.travelplatform.payment.api.dto;

import com.travelplatform.payment.domain.payment.Payment;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentId,
        String bookingId,
        String status,
        BigDecimal amount,
        String currency,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id().value().toString(),
                payment.bookingId().value().toString(),
                payment.status().name(),
                payment.amount().amount(),
                payment.amount().currency(),
                payment.createdAt());
    }
}

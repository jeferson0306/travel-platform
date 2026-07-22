package com.travelplatform.payment.infrastructure.persistence.mongo;

import com.travelplatform.payment.domain.payment.BookingId;
import com.travelplatform.payment.domain.payment.Money;
import com.travelplatform.payment.domain.payment.Payment;
import com.travelplatform.payment.domain.payment.PaymentId;
import com.travelplatform.payment.domain.payment.PaymentStatus;
import java.math.BigDecimal;
import java.util.Date;
import org.bson.Document;

public final class PaymentDocumentMapper {

    private PaymentDocumentMapper() {}

    public static Document toDocument(Payment payment) {
        return new Document("_id", payment.id().value().toString())
                .append("bookingId", payment.bookingId().value().toString())
                .append("amountValue", payment.amount().amount().toPlainString())
                .append("amountCurrency", payment.amount().currency())
                .append("status", payment.status().name())
                .append("createdAt", Date.from(payment.createdAt()));
    }

    public static Payment toDomain(Document document) {
        return Payment.reconstitute(
                PaymentId.of(document.getString("_id")),
                BookingId.of(document.getString("bookingId")),
                new Money(
                        new BigDecimal(document.getString("amountValue")),
                        document.getString("amountCurrency")),
                PaymentStatus.valueOf(document.getString("status")),
                document.getDate("createdAt").toInstant());
    }
}

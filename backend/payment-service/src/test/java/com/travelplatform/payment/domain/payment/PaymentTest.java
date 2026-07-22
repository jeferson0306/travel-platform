package com.travelplatform.payment.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final Money AMOUNT = new Money(new BigDecimal("450.00"), "EUR");

    @Test
    void authorizeRaisesPaymentAuthorizedAndStartsAuthorized() {
        var bookingId = new BookingId(UUID.randomUUID());

        var payment = Payment.authorize(bookingId, AMOUNT);

        assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.bookingId()).isEqualTo(bookingId);
        assertThat(payment.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(
                        PaymentAuthorized.class,
                        event -> {
                            assertThat(event.paymentId()).isEqualTo(payment.id());
                            assertThat(event.bookingId()).isEqualTo(bookingId);
                            assertThat(event.amount()).isEqualTo(AMOUNT);
                        });
    }

    @Test
    void failRaisesPaymentFailedAndStartsFailed() {
        var bookingId = new BookingId(UUID.randomUUID());

        var payment = Payment.fail(bookingId, AMOUNT, "declined by gateway");

        assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(
                        PaymentFailed.class,
                        event -> assertThat(event.reason()).isEqualTo("declined by gateway"));
    }

    @Test
    void refundRaisesPaymentRefundedAndChangesStatus() {
        var payment = Payment.authorize(new BookingId(UUID.randomUUID()), AMOUNT);
        payment.pullDomainEvents();

        payment.refund();

        assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(PaymentRefunded.class);
    }

    @Test
    void refundTwiceThrows() {
        var payment = Payment.authorize(new BookingId(UUID.randomUUID()), AMOUNT);
        payment.refund();

        assertThatThrownBy(payment::refund).isInstanceOf(PaymentAlreadyRefundedException.class);
    }

    @Test
    void refundingAFailedPaymentThrows() {
        var payment = Payment.fail(new BookingId(UUID.randomUUID()), AMOUNT, "declined");

        assertThatThrownBy(payment::refund).isInstanceOf(IllegalStateException.class);
    }
}

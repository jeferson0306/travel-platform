package com.travelplatform.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.payment.application.port.in.RefundPaymentUseCase.RefundPaymentCommand;
import com.travelplatform.payment.application.port.out.PaymentGateway;
import com.travelplatform.payment.application.port.out.PaymentRepository;
import com.travelplatform.payment.domain.payment.BookingId;
import com.travelplatform.payment.domain.payment.Money;
import com.travelplatform.payment.domain.payment.Payment;
import com.travelplatform.payment.domain.payment.PaymentId;
import com.travelplatform.payment.domain.payment.PaymentNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundPaymentServiceTest {

    private static final Money AMOUNT = new Money(new BigDecimal("450.00"), "EUR");

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentGateway paymentGateway;

    RefundPaymentService service;

    @BeforeEach
    void setUp() {
        service = new RefundPaymentService(paymentRepository, paymentGateway);
    }

    @Test
    void refundsAnAuthorizedPayment() {
        var bookingId = new BookingId(UUID.randomUUID());
        var payment = Payment.authorize(bookingId, AMOUNT);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        service.refund(new RefundPaymentCommand(bookingId.value().toString()));

        verify(paymentGateway).refund(payment.id(), AMOUNT);
        verify(paymentRepository).save(payment);
    }

    @Test
    void isANoOpForAFailedPayment() {
        var bookingId = new BookingId(UUID.randomUUID());
        var payment = Payment.fail(bookingId, AMOUNT, "declined");
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        service.refund(new RefundPaymentCommand(bookingId.value().toString()));

        verify(paymentGateway, never()).refund(any(PaymentId.class), any(Money.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void rejectsRefundingAnUnknownBooking() {
        var bookingId = UUID.randomUUID().toString();
        when(paymentRepository.findByBookingId(BookingId.of(bookingId)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refund(new RefundPaymentCommand(bookingId)))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}

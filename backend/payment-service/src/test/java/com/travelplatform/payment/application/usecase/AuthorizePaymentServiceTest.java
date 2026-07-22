package com.travelplatform.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelplatform.payment.application.port.in.AuthorizePaymentUseCase.AuthorizePaymentCommand;
import com.travelplatform.payment.application.port.out.PaymentGateway;
import com.travelplatform.payment.application.port.out.PaymentRepository;
import com.travelplatform.payment.domain.payment.Money;
import com.travelplatform.payment.domain.payment.Payment;
import com.travelplatform.payment.domain.payment.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizePaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentGateway paymentGateway;

    AuthorizePaymentService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizePaymentService(paymentRepository, paymentGateway);
    }

    @Test
    void savesAnAuthorizedPaymentWhenTheGatewayApproves() {
        when(paymentGateway.authorize(any(Money.class))).thenReturn(true);
        var bookingId = UUID.randomUUID().toString();

        service.authorize(new AuthorizePaymentCommand(bookingId, new BigDecimal("450.00"), "EUR"));

        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(captor.getValue().bookingId().value().toString()).isEqualTo(bookingId);
    }

    @Test
    void savesAFailedPaymentWhenTheGatewayDeclines() {
        when(paymentGateway.authorize(any(Money.class))).thenReturn(false);

        service.authorize(
                new AuthorizePaymentCommand(
                        UUID.randomUUID().toString(), new BigDecimal("450.00"), "EUR"));

        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.FAILED);
    }
}

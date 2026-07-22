package com.travelplatform.payment.application.usecase;

import com.travelplatform.payment.application.port.in.AuthorizePaymentUseCase;
import com.travelplatform.payment.application.port.out.PaymentGateway;
import com.travelplatform.payment.application.port.out.PaymentRepository;
import com.travelplatform.payment.domain.payment.BookingId;
import com.travelplatform.payment.domain.payment.Money;
import com.travelplatform.payment.domain.payment.Payment;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthorizePaymentService implements AuthorizePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public AuthorizePaymentService(
            PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public void authorize(AuthorizePaymentCommand command) {
        var bookingId = BookingId.of(command.bookingId());
        var amount = new Money(command.amount(), command.currency());

        var payment =
                paymentGateway.authorize(amount)
                        ? Payment.authorize(bookingId, amount)
                        : Payment.fail(bookingId, amount, "declined by gateway");

        paymentRepository.save(payment);
    }
}

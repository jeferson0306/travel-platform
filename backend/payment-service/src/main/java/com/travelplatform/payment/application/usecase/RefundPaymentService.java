package com.travelplatform.payment.application.usecase;

import com.travelplatform.payment.application.port.in.RefundPaymentUseCase;
import com.travelplatform.payment.application.port.out.PaymentGateway;
import com.travelplatform.payment.application.port.out.PaymentRepository;
import com.travelplatform.payment.domain.payment.BookingId;
import com.travelplatform.payment.domain.payment.PaymentNotFoundException;
import com.travelplatform.payment.domain.payment.PaymentStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public RefundPaymentService(
            PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    @Override
    public void refund(RefundPaymentCommand command) {
        var bookingId = BookingId.of(command.bookingId());
        var payment =
                paymentRepository
                        .findByBookingId(bookingId)
                        .orElseThrow(() -> new PaymentNotFoundException(bookingId));

        if (payment.status() != PaymentStatus.AUTHORIZED) {
            // FAILED - nothing was ever charged; REFUNDED - already handled. Either way, this is
            // an idempotent no-op, not an error.
            return;
        }

        payment.refund();
        paymentGateway.refund(payment.id(), payment.amount());
        paymentRepository.save(payment);
    }
}

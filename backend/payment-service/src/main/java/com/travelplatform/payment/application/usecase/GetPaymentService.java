package com.travelplatform.payment.application.usecase;

import com.travelplatform.payment.application.port.in.GetPaymentUseCase;
import com.travelplatform.payment.application.port.out.PaymentRepository;
import com.travelplatform.payment.domain.payment.BookingId;
import com.travelplatform.payment.domain.payment.Payment;
import com.travelplatform.payment.domain.payment.PaymentNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment getByBookingId(GetPaymentQuery query) {
        var bookingId = BookingId.of(query.bookingId());
        return paymentRepository
                .findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException(bookingId));
    }
}

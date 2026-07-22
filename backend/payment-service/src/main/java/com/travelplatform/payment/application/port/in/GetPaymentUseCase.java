package com.travelplatform.payment.application.port.in;

import com.travelplatform.payment.domain.payment.Payment;

public interface GetPaymentUseCase {

    Payment getByBookingId(GetPaymentQuery query);

    record GetPaymentQuery(String bookingId) {}
}

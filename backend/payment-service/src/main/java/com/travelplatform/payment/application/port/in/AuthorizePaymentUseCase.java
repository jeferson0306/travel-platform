package com.travelplatform.payment.application.port.in;

import java.math.BigDecimal;

/** Driven by booking-service's booking-created event (ROADMAP M11), not a public endpoint. */
public interface AuthorizePaymentUseCase {

    void authorize(AuthorizePaymentCommand command);

    record AuthorizePaymentCommand(String bookingId, BigDecimal amount, String currency) {}
}

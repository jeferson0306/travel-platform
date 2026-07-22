package com.travelplatform.payment.api.resource;

import com.travelplatform.payment.api.dto.PaymentResponse;
import com.travelplatform.payment.application.port.in.GetPaymentUseCase;
import com.travelplatform.payment.application.port.in.GetPaymentUseCase.GetPaymentQuery;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Read-only: payments are only ever created/refunded by this service's own Kafka consumers
 * (booking-created/booking-cancelled - ROADMAP M11), never by a direct API call. Restricted to
 * support/admin roles (see docs/adr/0006-rbac-roles.md) - a traveler checks payment status via
 * booking-service, not this service directly.
 */
@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentResource(GetPaymentUseCase getPaymentUseCase) {
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @GET
    @Path("/{bookingId}")
    @RolesAllowed({"SUPPORT", "ADMIN", "SUPER_ADMIN"})
    public PaymentResponse getByBookingId(@PathParam("bookingId") String bookingId) {
        var payment = getPaymentUseCase.getByBookingId(new GetPaymentQuery(bookingId));
        return PaymentResponse.from(payment);
    }
}

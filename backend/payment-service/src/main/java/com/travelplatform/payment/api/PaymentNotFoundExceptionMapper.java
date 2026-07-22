package com.travelplatform.payment.api;

import com.travelplatform.payment.api.dto.ErrorResponse;
import com.travelplatform.payment.domain.payment.PaymentNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PaymentNotFoundExceptionMapper implements ExceptionMapper<PaymentNotFoundException> {

    @Override
    public Response toResponse(PaymentNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", exception.getMessage()))
                .build();
    }
}

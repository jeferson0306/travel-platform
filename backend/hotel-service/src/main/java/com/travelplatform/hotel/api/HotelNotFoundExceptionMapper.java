package com.travelplatform.hotel.api;

import com.travelplatform.hotel.api.dto.ErrorResponse;
import com.travelplatform.hotel.domain.hotel.HotelNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class HotelNotFoundExceptionMapper implements ExceptionMapper<HotelNotFoundException> {

    @Override
    public Response toResponse(HotelNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", exception.getMessage()))
                .build();
    }
}

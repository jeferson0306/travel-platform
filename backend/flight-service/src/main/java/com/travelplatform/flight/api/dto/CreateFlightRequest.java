package com.travelplatform.flight.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateFlightRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code")
                String origin,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code")
                String destination,
        @NotNull @Future Instant departureAt,
        @NotNull @Future Instant arrivalAt,
        @NotNull @PositiveOrZero BigDecimal priceAmount,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter ISO 4217 code")
                String priceCurrency,
        @Min(0) int availableSeats,
        @NotBlank String airline,
        @NotBlank String airlineCode,
        @NotBlank String flightNumber,
        String cabinClass,
        @Min(0) int stops) {}

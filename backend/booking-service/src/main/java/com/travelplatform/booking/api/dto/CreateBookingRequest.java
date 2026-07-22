package com.travelplatform.booking.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * travelerId/amount/currency/travelerEmail are trusted client input for now (see BookingResource) -
 * booking-service does not yet look up the authoritative price from flight-service/hotel-service or
 * the traveler's email from identity-service synchronously. Revisit once the Gateway (ROADMAP M14)
 * or a pricing/identity lookup lands.
 */
public record CreateBookingRequest(
        @NotBlank
                @Pattern(
                        regexp =
                                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                        message = "must be a well-formed UUID")
                String travelerId,
        @NotBlank @Email String travelerEmail,
        @NotBlank @Pattern(regexp = "^(FLIGHT|HOTEL)$", message = "must be FLIGHT or HOTEL")
                String itemType,
        @NotBlank
                @Pattern(
                        regexp =
                                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                        message = "must be a well-formed UUID")
                String itemId,
        @Min(value = 1, message = "must be at least 1") int quantity,
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter ISO 4217 code")
                String currency) {}

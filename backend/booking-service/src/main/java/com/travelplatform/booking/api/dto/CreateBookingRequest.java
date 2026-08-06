package com.travelplatform.booking.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * travelerId is no longer client input - BookingResource derives it from the verified JWT subject.
 * amount/currency/travelerEmail are still trusted client input for now - booking-service does not
 * yet look up the authoritative price from flight-service/hotel-service or the traveler's email
 * from identity-service synchronously. That remains a known gap (see BookingResource), tracked
 * separately from the travelerId fix. itemSummary is likewise trusted, optional client input: a
 * short human-readable description of the item (e.g. "Lisbon -> Sao Paulo, TP123, TAP Air Portugal"
 * for a flight, or "Lisbon Riverside Hotel, Lisbon" for a hotel) that the frontend already has in
 * hand from the search results the user just clicked. It is descriptive/UI-only, so it carries no
 * domain-level validation and a booking created without it still works exactly as before.
 */
public record CreateBookingRequest(
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
                String currency,
        String itemSummary) {}

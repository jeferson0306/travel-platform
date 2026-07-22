package com.travelplatform.booking.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBookingRequest(
        @NotBlank
                @Pattern(
                        regexp =
                                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                        message = "must be a well-formed UUID")
                String travelerId,
        @NotBlank @Pattern(regexp = "^(FLIGHT|HOTEL)$", message = "must be FLIGHT or HOTEL")
                String itemType,
        @NotBlank
                @Pattern(
                        regexp =
                                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                        message = "must be a well-formed UUID")
                String itemId,
        @Min(value = 1, message = "must be at least 1") int quantity) {}

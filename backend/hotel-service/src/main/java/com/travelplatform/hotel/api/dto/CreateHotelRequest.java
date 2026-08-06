package com.travelplatform.hotel.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateHotelRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String city,
        @NotNull @PositiveOrZero BigDecimal pricePerNightAmount,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter ISO 4217 code")
                String pricePerNightCurrency,
        @Min(0) int availableRooms,
        @Size(max = 200) String address,
        @Min(1) @Max(5) Integer starRating,
        List<String> amenities,
        @Size(max = 2000) String description,
        Double reviewScore,
        int reviewCount) {}

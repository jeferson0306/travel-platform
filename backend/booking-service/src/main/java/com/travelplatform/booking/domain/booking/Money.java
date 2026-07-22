package com.travelplatform.booking.domain.booking;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** An amount in a currency - what a booking costs, charged via payment-service (ROADMAP M11). */
public record Money(BigDecimal amount, String currency) {

    private static final Pattern CURRENCY_FORMAT = Pattern.compile("^[A-Z]{3}$");

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        currency = currency.strip().toUpperCase(Locale.ROOT);
        if (!CURRENCY_FORMAT.matcher(currency).matches()) {
            throw new IllegalArgumentException("Invalid ISO 4217 currency code: " + currency);
        }
    }
}

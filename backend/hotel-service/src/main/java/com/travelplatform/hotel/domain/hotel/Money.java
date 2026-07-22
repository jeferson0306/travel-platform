package com.travelplatform.hotel.domain.hotel;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** An amount in a currency, per night. Multi-currency conversion is not implemented yet. */
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

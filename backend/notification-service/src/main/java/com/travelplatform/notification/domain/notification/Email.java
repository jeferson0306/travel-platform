package com.travelplatform.notification.domain.notification;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Duplicated per service on purpose - no shared runtime code between services (ADR 0005). */
public record Email(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(value, "value must not be null");
        value = value.strip().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
}

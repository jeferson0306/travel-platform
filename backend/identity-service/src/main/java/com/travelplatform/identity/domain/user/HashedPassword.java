package com.travelplatform.identity.domain.user;

import java.util.Objects;

/** An already-hashed password. The domain never sees or stores a raw password. */
public record HashedPassword(String value) {

    public HashedPassword {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}

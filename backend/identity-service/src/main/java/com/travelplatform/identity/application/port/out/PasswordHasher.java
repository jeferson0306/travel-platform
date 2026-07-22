package com.travelplatform.identity.application.port.out;

import com.travelplatform.identity.domain.user.HashedPassword;

/** Outbound port for password hashing. Implemented by an infrastructure adapter. */
public interface PasswordHasher {

    HashedPassword hash(String rawPassword);

    boolean matches(String rawPassword, HashedPassword hashedPassword);
}

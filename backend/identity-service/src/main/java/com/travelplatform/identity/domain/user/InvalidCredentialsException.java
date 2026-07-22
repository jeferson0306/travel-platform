package com.travelplatform.identity.domain.user;

import com.travelplatform.identity.domain.shared.DomainException;

public final class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}

package com.travelplatform.identity.domain.user;

import com.travelplatform.identity.domain.shared.DomainException;

public final class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException(Email email) {
        super("Email already registered: " + email.value());
    }
}

package com.travelplatform.identity.domain.shared;

/** Base type for rule violations raised by the domain layer. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}

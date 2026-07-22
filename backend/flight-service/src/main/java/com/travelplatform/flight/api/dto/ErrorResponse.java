package com.travelplatform.flight.api.dto;

import java.util.List;

/**
 * Canonical error shape shared by every service. See docs/architecture/error-handling.md.
 * Duplicated per service on purpose - no shared runtime code between services (ADR 0005).
 */
public record ErrorResponse(String error, String message, List<String> details) {

    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}

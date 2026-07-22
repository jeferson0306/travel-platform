package com.travelplatform.identity.api.dto;

import java.util.List;

/** Canonical error shape shared by every service. See docs/architecture/error-handling.md. */
public record ErrorResponse(String error, String message, List<String> details) {

    public ErrorResponse(String error, String message) {
        this(error, message, null);
    }
}

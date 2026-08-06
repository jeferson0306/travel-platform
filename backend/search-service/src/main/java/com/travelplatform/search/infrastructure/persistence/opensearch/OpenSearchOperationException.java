package com.travelplatform.search.infrastructure.persistence.opensearch;

/**
 * Wraps the checked {@link java.io.IOException} the OpenSearch client throws on any I/O failure.
 */
public class OpenSearchOperationException extends RuntimeException {

    public OpenSearchOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

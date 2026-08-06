package com.travelplatform.search.infrastructure.observability;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * Populates the fields every request log line must carry (see the observability contract in
 * ARCHITECTURE.md): correlationId, requestId, method, uri, status and durationMs. No {@code userId}
 * field - unlike every other service, search-service has no authenticated endpoints at all (search
 * is public, per ADR 0012), so there is never a principal to report.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String START_NANOS_PROPERTY = "requestLogging.startNanos";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        MDC.put("requestId", UUID.randomUUID().toString());
        MDC.put("method", requestContext.getMethod());
        MDC.put("uri", requestContext.getUriInfo().getPath());

        requestContext.setProperty(START_NANOS_PROPERTY, System.nanoTime());
    }

    @Override
    public void filter(
            ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var startNanos = (Long) requestContext.getProperty(START_NANOS_PROPERTY);
        var durationMs = startNanos == null ? -1 : (System.nanoTime() - startNanos) / 1_000_000;

        MDC.put("status", responseContext.getStatus());
        MDC.put("durationMs", durationMs);
        responseContext.getHeaders().add(CORRELATION_ID_HEADER, MDC.get("correlationId"));

        LOG.info("request completed");
        MDC.clear();
    }
}

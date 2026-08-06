package com.travelplatform.gateway.proxy;

import com.travelplatform.gateway.config.RoutingTable;
import com.travelplatform.gateway.ratelimit.RateLimiter;
import com.travelplatform.gateway.security.TokenValidator;
import io.quarkus.vertx.web.Route;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The gateway's single reverse-proxy route: everything under {@code /api/v1/} is forwarded verbatim
 * (method, headers, query string, body) to whichever backend owns that path's first segment (see
 * {@link RoutingTable}), and the upstream response is streamed back verbatim. No path rewriting
 * happens - every backend already exposes the exact same {@code /api/v1/...} path it is reached at
 * through the gateway, so the full request path is forwarded unchanged.
 *
 * <p>Order of checks, cheapest first: rate limit, then JWT fast-fail (see {@link TokenValidator}),
 * then forward via {@link UpstreamProxyClient} (timeout/circuit-breaker/bulkhead per backend, retry
 * only for idempotent GET/HEAD - see docs/adr/0014-fault-tolerance.md). Runs on a worker thread
 * ({@code type = BLOCKING}): {@link UpstreamProxyClient} blocks while a SmallRye {@code Guard}
 * invocation is in flight, which would otherwise stall the Vert.x event loop.
 */
@ApplicationScoped
public class ProxyRoutes {

    private static final Logger LOG = Logger.getLogger(ProxyRoutes.class);
    private static final Set<String> HOP_BY_HOP_HEADERS =
            Set.of("connection", "content-length", "host", "transfer-encoding");
    private static final Set<HttpMethod> IDEMPOTENT_METHODS =
            Set.of(HttpMethod.GET, HttpMethod.HEAD);
    private static final String CORS_ALLOWED_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private static final String CORS_ALLOWED_HEADERS =
            "Authorization,Content-Type,X-Correlation-Id";
    private static final String CORS_EXPOSED_HEADERS = "X-Correlation-Id";

    private final RoutingTable routingTable;
    private final RateLimiter rateLimiter;
    private final TokenValidator tokenValidator;
    private final UpstreamProxyClient upstreamProxyClient;
    private final List<String> allowedOrigins;

    public ProxyRoutes(
            RoutingTable routingTable,
            RateLimiter rateLimiter,
            TokenValidator tokenValidator,
            UpstreamProxyClient upstreamProxyClient,
            @ConfigProperty(name = "gateway.cors.allowed-origins") String allowedOriginsConfig) {
        this.routingTable = routingTable;
        this.rateLimiter = rateLimiter;
        this.tokenValidator = tokenValidator;
        this.upstreamProxyClient = upstreamProxyClient;
        this.allowedOrigins =
                Arrays.stream(allowedOriginsConfig.split(",")).map(String::trim).toList();
    }

    /**
     * Quarkus's own declarative CORS filter proved unreliable on this version (see the comment in
     * application.yml), so CORS is handled here instead: matched explicitly against the configured
     * allow-list rather than rejected with a framework-level 403. An origin that isn't on the list
     * simply doesn't get the header - the browser enforces the block client-side, which is the
     * spec-correct behavior anyway.
     */
    private String matchedOrigin(RoutingContext rc) {
        var origin = rc.request().getHeader("Origin");
        return origin != null && allowedOrigins.contains(origin) ? origin : null;
    }

    private void applyCorsHeaders(RoutingContext rc, String origin) {
        rc.response()
                .putHeader("Access-Control-Allow-Origin", origin)
                .putHeader("Access-Control-Allow-Credentials", "true")
                .putHeader("Vary", "Origin")
                .putHeader("Access-Control-Expose-Headers", CORS_EXPOSED_HEADERS);
    }

    @Route(
            regex = "/api/v1/.*",
            type = Route.HandlerType.BLOCKING,
            methods = Route.HttpMethod.OPTIONS)
    void preflight(RoutingContext rc) {
        var origin = matchedOrigin(rc);
        var response = rc.response().setStatusCode(200);
        if (origin != null) {
            response.putHeader("Access-Control-Allow-Origin", origin)
                    .putHeader("Access-Control-Allow-Credentials", "true")
                    .putHeader("Vary", "Origin");
        }
        response.putHeader("Access-Control-Allow-Methods", CORS_ALLOWED_METHODS)
                .putHeader("Access-Control-Allow-Headers", CORS_ALLOWED_HEADERS)
                .end();
    }

    @Route(
            regex = "/api/v1/[^/]+.*",
            type = Route.HandlerType.BLOCKING,
            methods = {
                Route.HttpMethod.GET,
                Route.HttpMethod.POST,
                Route.HttpMethod.PUT,
                Route.HttpMethod.PATCH,
                Route.HttpMethod.DELETE
            })
    void proxy(RoutingContext rc) {
        var origin = matchedOrigin(rc);
        if (origin != null) {
            applyCorsHeaders(rc, origin);
        }

        var segment = firstSegmentAfterApiV1(rc.request().path());
        var upstreamBaseUrl = routingTable.resolve(segment);
        if (upstreamBaseUrl == null) {
            rc.response().setStatusCode(404).end();
            return;
        }

        var clientKey = rc.request().remoteAddress().host();
        boolean allowed;
        try {
            allowed = rateLimiter.allow(clientKey).await().indefinitely();
        } catch (RuntimeException e) {
            LOG.error("Rate limiter check failed", e);
            rc.response().setStatusCode(502).end("Rate limiter unavailable");
            return;
        }
        if (!allowed) {
            rc.response().setStatusCode(429).end("Rate limit exceeded");
            return;
        }

        if (!tokenValidator.isValid(rc.request().getHeader("Authorization"))) {
            rc.response().setStatusCode(401).end("Invalid or expired token");
            return;
        }

        forward(rc, segment, upstreamBaseUrl);
    }

    private void forward(RoutingContext rc, String segment, String upstreamBaseUrl) {
        var targetUri = upstreamBaseUrl + rc.request().uri();
        var method = rc.request().method();

        var forwardedHeaders = MultiMap.caseInsensitiveMultiMap();
        rc.request()
                .headers()
                .forEach(
                        entry -> {
                            if (!HOP_BY_HOP_HEADERS.contains(
                                    entry.getKey().toLowerCase(Locale.ROOT))) {
                                forwardedHeaders.add(entry.getKey(), entry.getValue());
                            }
                        });

        var coreBody = rc.body().buffer();
        var body =
                coreBody == null
                        ? io.vertx.mutiny.core.buffer.Buffer.buffer()
                        : io.vertx.mutiny.core.buffer.Buffer.newInstance(coreBody);

        try {
            var upstreamResponse =
                    IDEMPOTENT_METHODS.contains(method)
                            ? upstreamProxyClient.forwardIdempotent(
                                    segment, method, targetUri, forwardedHeaders, body)
                            : upstreamProxyClient.forwardNonIdempotent(
                                    segment, method, targetUri, forwardedHeaders, body);

            var proxied = rc.response().setStatusCode(upstreamResponse.statusCode());
            upstreamResponse
                    .headers()
                    .forEach(
                            entry -> {
                                if (!HOP_BY_HOP_HEADERS.contains(
                                        entry.getKey().toLowerCase(Locale.ROOT))) {
                                    proxied.putHeader(entry.getKey(), entry.getValue());
                                }
                            });
            // bodyAsBuffer() returns null for a genuinely empty upstream body (e.g. a 204, or a
            // 201 whose body hasn't fully arrived when this is read) - observed directly as an
            // NPE here under real traffic, not just a theoretical case. Same defensive pattern as
            // the request body above: null means "nothing to write", not "something went wrong".
            var upstreamBody = upstreamResponse.bodyAsBuffer();
            if (upstreamBody == null) {
                proxied.end();
            } else {
                proxied.end(upstreamBody.getDelegate());
            }
        } catch (Exception e) {
            LOG.error("Failed to reach upstream " + targetUri, e);
            var outcome = outcomeFor(e);
            rc.response().setStatusCode(outcome.status()).end(outcome.message());
        }
    }

    /**
     * Distinguishes why the upstream call failed with the matching HTTP semantics, instead of a
     * blanket 502 for everything - see docs/adr/0014-fault-tolerance.md. A fixed message per case
     * (not {@code failure.getMessage()}) so an internal exception's class name/detail never leaks
     * to an API client.
     */
    private static FailureOutcome outcomeFor(Throwable failure) {
        if (failure
                instanceof
                org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException) {
            return new FailureOutcome(503, "Upstream circuit breaker open");
        }
        if (failure
                instanceof org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException) {
            return new FailureOutcome(504, "Upstream timeout");
        }
        return new FailureOutcome(502, "Upstream unavailable");
    }

    private record FailureOutcome(int status, String message) {}

    private static String firstSegmentAfterApiV1(String path) {
        var rest = path.substring("/api/v1/".length());
        var slash = rest.indexOf('/');
        return slash == -1 ? rest : rest.substring(0, slash);
    }
}

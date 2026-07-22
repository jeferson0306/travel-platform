package com.travelplatform.gateway.proxy;

import com.travelplatform.gateway.config.RoutingTable;
import com.travelplatform.gateway.ratelimit.RateLimiter;
import com.travelplatform.gateway.security.TokenValidator;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import org.jboss.logging.Logger;

/**
 * The gateway's single reverse-proxy route: everything under {@code /api/v1/} is forwarded verbatim
 * (method, headers, query string, body) to whichever backend owns that path's first segment (see
 * {@link RoutingTable}), and the upstream response is streamed back verbatim. No path rewriting
 * happens - every backend already exposes the exact same {@code /api/v1/...} path it is reached at
 * through the gateway, so the full request path is forwarded unchanged.
 *
 * <p>Order of checks, cheapest first: rate limit, then JWT fast-fail (see {@link TokenValidator}),
 * then forward - see docs/adr/0013-api-gateway.md.
 */
@ApplicationScoped
public class ProxyRoutes {

    private static final Logger LOG = Logger.getLogger(ProxyRoutes.class);
    private static final Set<String> HOP_BY_HOP_HEADERS =
            Set.of("connection", "content-length", "host", "transfer-encoding");

    private final RoutingTable routingTable;
    private final RateLimiter rateLimiter;
    private final TokenValidator tokenValidator;
    private final WebClient webClient;

    public ProxyRoutes(
            RoutingTable routingTable,
            RateLimiter rateLimiter,
            TokenValidator tokenValidator,
            WebClient webClient) {
        this.routingTable = routingTable;
        this.rateLimiter = rateLimiter;
        this.tokenValidator = tokenValidator;
        this.webClient = webClient;
    }

    @Route(
            regex = "/api/v1/[^/]+.*",
            methods = {
                Route.HttpMethod.GET,
                Route.HttpMethod.POST,
                Route.HttpMethod.PUT,
                Route.HttpMethod.PATCH,
                Route.HttpMethod.DELETE
            })
    void proxy(RoutingContext rc) {
        var path = rc.request().path();
        var segment = firstSegmentAfterApiV1(path);
        var upstreamBaseUrl = routingTable.resolve(segment);
        if (upstreamBaseUrl == null) {
            rc.response().setStatusCode(404).end();
            return;
        }

        var clientKey = rc.request().remoteAddress().host();
        rateLimiter
                .allow(clientKey)
                .subscribe()
                .with(
                        allowed -> {
                            if (!allowed) {
                                rc.response().setStatusCode(429).end("Rate limit exceeded");
                                return;
                            }
                            if (!tokenValidator.isValid(rc.request().getHeader("Authorization"))) {
                                rc.response().setStatusCode(401).end("Invalid or expired token");
                                return;
                            }
                            forward(rc, upstreamBaseUrl);
                        },
                        failure -> {
                            LOG.error("Rate limiter check failed", failure);
                            rc.response().setStatusCode(502).end();
                        });
    }

    private void forward(RoutingContext rc, String upstreamBaseUrl) {
        var targetUri = upstreamBaseUrl + rc.request().uri();
        var request =
                webClient.requestAbs(HttpMethod.valueOf(rc.request().method().name()), targetUri);

        rc.request()
                .headers()
                .forEach(
                        entry -> {
                            if (!HOP_BY_HOP_HEADERS.contains(
                                    entry.getKey().toLowerCase(java.util.Locale.ROOT))) {
                                request.putHeader(entry.getKey(), entry.getValue());
                            }
                        });

        var coreBody = rc.body().buffer();
        var body =
                coreBody == null
                        ? io.vertx.mutiny.core.buffer.Buffer.buffer()
                        : io.vertx.mutiny.core.buffer.Buffer.newInstance(coreBody);
        request.sendBuffer(body)
                .subscribe()
                .with(
                        response -> {
                            var proxied = rc.response().setStatusCode(response.statusCode());
                            response.headers()
                                    .forEach(
                                            entry -> {
                                                if (!HOP_BY_HOP_HEADERS.contains(
                                                        entry.getKey()
                                                                .toLowerCase(
                                                                        java.util.Locale.ROOT))) {
                                                    proxied.putHeader(
                                                            entry.getKey(), entry.getValue());
                                                }
                                            });
                            proxied.end(response.bodyAsBuffer().getDelegate());
                        },
                        failure -> {
                            LOG.error("Failed to reach upstream " + targetUri, failure);
                            rc.response().setStatusCode(502).end("Upstream unavailable");
                        });
    }

    private static String firstSegmentAfterApiV1(String path) {
        var rest = path.substring("/api/v1/".length());
        var slash = rest.indexOf('/');
        return slash == -1 ? rest : rest.substring(0, slash);
    }
}

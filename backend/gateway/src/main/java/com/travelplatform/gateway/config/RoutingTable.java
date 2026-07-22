package com.travelplatform.gateway.config;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Maps the first path segment after {@code /api/v1/} to the backend service that owns it. One entry
 * per resource prefix (see each service's own {@code @Path}, not a guess) - not a convention-based
 * rule, since nothing in this platform enforces service name == path segment (e.g. {@code
 * /api/v1/auth} routes to {@code identity-service}).
 */
@ApplicationScoped
public class RoutingTable {

    private final Map<String, String> baseUrlsBySegment;

    public RoutingTable(
            @ConfigProperty(name = "gateway.upstream.identity") String identity,
            @ConfigProperty(name = "gateway.upstream.booking") String booking,
            @ConfigProperty(name = "gateway.upstream.flight") String flight,
            @ConfigProperty(name = "gateway.upstream.hotel") String hotel,
            @ConfigProperty(name = "gateway.upstream.payment") String payment,
            @ConfigProperty(name = "gateway.upstream.notification") String notification,
            @ConfigProperty(name = "gateway.upstream.search") String search) {
        this.baseUrlsBySegment =
                Map.ofEntries(
                        Map.entry("auth", identity),
                        Map.entry("bookings", booking),
                        Map.entry("flights", flight),
                        Map.entry("hotels", hotel),
                        Map.entry("payments", payment),
                        Map.entry("notifications", notification),
                        Map.entry("search", search));
    }

    /**
     * Returns the upstream base URL for a path's first segment after {@code /api/v1/}, or {@code
     * null} if no service owns that segment.
     */
    public String resolve(String segment) {
        return baseUrlsBySegment.get(segment);
    }
}

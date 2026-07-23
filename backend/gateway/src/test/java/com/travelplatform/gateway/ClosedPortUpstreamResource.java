package com.travelplatform.gateway;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Remaps "hotels" to a port nothing is listening on, so every call fails immediately with a
 * connection error - used to drive {@link com.travelplatform.gateway.proxy.UpstreamProxyClient}'s
 * per-backend circuit breaker into the open state deterministically (a real timeout would work too,
 * but would make the test slow and would also engage the retry configured on GETs - see
 * docs/adr/0014-fault-tolerance.md for why POST/{@code forwardNonIdempotent} is used for this
 * instead).
 */
public class ClosedPortUpstreamResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to find a free port to close", e);
        }
        // The socket above is already closed by the try-with-resources - nothing listens on
        // freePort now, so any connection to it fails immediately.
        return Map.of("gateway.upstream.hotel", "http://localhost:" + freePort);
    }

    @Override
    public void stop() {}
}

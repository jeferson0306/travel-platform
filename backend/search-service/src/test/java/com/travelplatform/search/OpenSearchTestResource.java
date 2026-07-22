package com.travelplatform.search;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a real OpenSearch node via Testcontainers for the test suite - this service has no Quarkus
 * Dev Services support to fall back on, unlike MongoDB/Kafka elsewhere in this platform (see
 * docs/adr/0012-search-service-opensearch.md). Security plugin is left disabled (the container's
 * default), matching the docker-compose setup used for local development.
 */
public class OpenSearchTestResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName IMAGE =
            DockerImageName.parse("opensearchproject/opensearch:3.7.0");

    private OpenSearchContainer<?> container;

    @Override
    public Map<String, String> start() {
        container = new OpenSearchContainer<>(IMAGE);
        container.start();

        // getHttpHostAddress() returns "http://host:port" - strip the scheme before splitting.
        var address = container.getHttpHostAddress().replaceFirst("^\\w+://", "");
        var parts = address.split(":");
        return Map.of(
                "search.opensearch.host", parts[0],
                "search.opensearch.port", parts[1],
                "search.opensearch.scheme", "http");
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}

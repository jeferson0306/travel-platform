package com.travelplatform.search.infrastructure.persistence.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.apache.hc.core5.http.HttpHost;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

/**
 * No Quarkus extension for OpenSearch exists at this platform's Quarkus version (same situation as
 * AWS S3 - ADR 0008), so the client is built and wired by hand instead of via dependency injection
 * magic - see docs/adr/0012-search-service-opensearch.md.
 */
@ApplicationScoped
public class OpenSearchClientProducer {

    @Produces
    @Singleton
    public OpenSearchClient client(
            @ConfigProperty(name = "search.opensearch.host", defaultValue = "localhost")
                    String host,
            @ConfigProperty(name = "search.opensearch.port", defaultValue = "9200") int port,
            @ConfigProperty(name = "search.opensearch.scheme", defaultValue = "http")
                    String scheme) {
        // The client's default JacksonJsonpMapper serializes Instant as a fractional epoch-seconds
        // number, which OpenSearch's "date" field type cannot parse - register JavaTimeModule and
        // write ISO-8601 strings instead, matching the "date" mapping's default accepted format.
        var objectMapper =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OpenSearchTransport transport =
                ApacheHttpClient5TransportBuilder.builder(new HttpHost(scheme, host, port))
                        .setMapper(new JacksonJsonpMapper(objectMapper))
                        .build();
        return new OpenSearchClient(transport);
    }
}

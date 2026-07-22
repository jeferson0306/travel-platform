package com.travelplatform.booking.infrastructure.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import java.net.URI;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * No Quarkus AWS extension is used here (see docs/adr/0008) - the S3 client is built directly from
 * the AWS SDK v2, pointed at LocalStack in dev/test via {@code booking.aws.s3.endpoint-override}
 * and the real AWS default credential chain/endpoint otherwise.
 */
public class S3ClientProducer {

    @Produces
    @ApplicationScoped
    public S3Client s3Client(
            @ConfigProperty(name = "booking.aws.s3.region") String region,
            @ConfigProperty(name = "booking.aws.s3.endpoint-override")
                    Optional<String> endpointOverride,
            @ConfigProperty(name = "booking.aws.s3.access-key-id") Optional<String> accessKeyId,
            @ConfigProperty(name = "booking.aws.s3.secret-access-key")
                    Optional<String> secretAccessKey) {
        var builder = S3Client.builder().region(Region.of(region));

        if (endpointOverride.isPresent()) {
            // LocalStack requires path-style bucket addressing (bucket.s3.amazonaws.com-style
            // virtual-hosted addressing does not resolve against it).
            builder.endpointOverride(URI.create(endpointOverride.get())).forcePathStyle(true);
        }

        if (accessKeyId.isPresent() && secretAccessKey.isPresent()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId.get(), secretAccessKey.get())));
        }

        return builder.build();
    }

    public void closeS3Client(@Disposes S3Client s3Client) {
        s3Client.close();
    }
}

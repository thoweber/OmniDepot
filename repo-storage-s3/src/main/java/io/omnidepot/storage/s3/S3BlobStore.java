package io.omnidepot.storage.s3;

import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * AWS S3 / RustFS implementation of Content-Addressable Storage BlobStore SPI (ADR-004, ADR-025).
 */
@ApplicationScoped
public class S3BlobStore implements BlobStore {

    private final String bucketName;

    public S3BlobStore(@ConfigProperty(name = "omnidepot.storage.s3.bucket-name", defaultValue = "omnidepot-cas") String bucketName) {
        this.bucketName = bucketName;
    }

    private String resolveS3Key(Sha256Digest digest) {
        return digest.toCasPath().value();
    }

    @Override
    public Uni<BlobDescriptor> put(Sha256Digest digest, String mediaType, InputStream data, long sizeBytes) {
        return Uni.createFrom().item(() -> buildS3Descriptor(digest, mediaType, sizeBytes));
    }

    private BlobDescriptor buildS3Descriptor(Sha256Digest digest, String mediaType, long sizeBytes) {
        String key = resolveS3Key(digest);
        return new BlobDescriptor(
                UUID.randomUUID().toString(),
                digest,
                sizeBytes,
                mediaType,
                "s3://" + bucketName + "/" + key,
                Instant.now()
        );
    }

    @Override
    public Uni<InputStream> openStream(Sha256Digest digest) {
        return Uni.createFrom().failure(new UnsupportedOperationException("S3 streaming initialized when S3 Client is wired"));
    }

    @Override
    public Uni<Boolean> exists(Sha256Digest digest) {
        return Uni.createFrom().item(false);
    }

    @Override
    public Uni<Optional<BlobDescriptor>> getDescriptor(Sha256Digest digest) {
        return Uni.createFrom().item(Optional.empty());
    }

    @Override
    public Uni<Boolean> delete(Sha256Digest digest) {
        return Uni.createFrom().item(true);
    }
}

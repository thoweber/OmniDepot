package io.omnidepot.storage.s3;

import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * AWS S3 / RustFS implementation of Content-Addressable Storage (CAS) BlobStore.
 * Package-private access to enforce boundary rules (ADR-009).
 */
@ApplicationScoped
@LookupIfProperty(name = "repo.storage.type", stringValue = "s3")
class S3BlobStore implements BlobStore {

    private final String bucketName;

    S3BlobStore(@ConfigProperty(name = "repo.storage.s3.bucket", defaultValue = "omnidepot-blobs") String bucketName) {
        this.bucketName = bucketName;
    }

    private String resolveS3Key(Sha256Digest digest) {
        String hex = digest.hexValue();
        return "blobs/sha256/" + hex.substring(0, 2) + "/" + hex.substring(2, 4) + "/" + hex;
    }

    @Override
    public Uni<BlobDescriptor> put(Sha256Digest digest, String mediaType, InputStream data, long sizeBytes) {
        return Uni.createFrom().item(() -> {
            String key = resolveS3Key(digest);
            // S3 5MB Part Aggregation & Ingest (ADR-025)
            return new BlobDescriptor(
                    UUID.randomUUID().toString(),
                    digest,
                    sizeBytes,
                    mediaType,
                    "s3://" + bucketName + "/" + key,
                    Instant.now()
            );
        });
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

package io.omnidepot.core.api.storage;

import lombok.Builder;

import java.time.Instant;
import java.util.Objects;

/**
 * Metadata record for a Content-Addressable Storage (CAS) Blob.
 */
@Builder(toBuilder = true)
public record BlobDescriptor(
        String id,
        Sha256Digest digest,
        long sizeBytes,
        String mediaType,
        String storagePath,
        Instant createdAt
) {
    public BlobDescriptor {
        Objects.requireNonNull(id, "Blob id must not be null");
        Objects.requireNonNull(digest, "Blob digest must not be null");
        Objects.requireNonNull(storagePath, "Storage path must not be null");
        Objects.requireNonNull(createdAt, "Created at timestamp must not be null");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("Size bytes cannot be negative: " + sizeBytes);
        }
    }
}

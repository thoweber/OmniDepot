package io.omnidepot.core.api.oci;

import io.omnidepot.core.api.storage.Sha256Digest;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable value object representing a stored OCI image manifest.
 */
public record StoredManifestRecord(
        String id,
        String repositoryName,
        Sha256Digest digest,
        String mediaType,
        long sizeBytes,
        String payload,
        Instant createdAt
) {
    public StoredManifestRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(repositoryName, "repositoryName must not be null");
        Objects.requireNonNull(digest, "digest must not be null");
        Objects.requireNonNull(mediaType, "mediaType must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}

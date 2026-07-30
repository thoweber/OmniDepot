package io.omnidepot.core.api.oci;

import io.smallrye.mutiny.Uni;

import java.util.Optional;

/**
 * Storage SPI for OCI manifest persistence, digest retrieval, and tag indexing (ADR-004, ADR-015, ADR-023).
 */
public interface ManifestStore {

    /**
     * Save or update an OCI manifest payload and link it to a repository and tag or digest reference.
     */
    Uni<StoredManifestRecord> saveManifest(String repositoryName, String reference, String mediaType, String payload);

    /**
     * Retrieve a stored OCI manifest by repository name and reference (tag name or sha256 digest).
     */
    Uni<Optional<StoredManifestRecord>> findManifest(String repositoryName, String reference);

    /**
     * Check if an OCI manifest exists for a repository name and reference.
     */
    Uni<Boolean> manifestExists(String repositoryName, String reference);
}

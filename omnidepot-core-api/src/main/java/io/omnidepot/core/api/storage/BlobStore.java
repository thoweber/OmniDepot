package io.omnidepot.core.api.storage;

import io.smallrye.mutiny.Uni;
import java.io.InputStream;
import java.util.Optional;

/**
 * Storage SPI for Content-Addressable Storage (CAS) operations.
 * Concrete implementations (Filesystem, AWS S3/RustFS) are injected dynamically.
 */
public interface BlobStore {

    /**
     * Store payload stream under its SHA-256 digest address.
     */
    Uni<BlobDescriptor> put(Sha256Digest digest, String mediaType, InputStream dataSupplier, long sizeBytes);

    /**
     * Open a read stream for a stored blob by digest.
     */
    Uni<InputStream> openStream(Sha256Digest digest);

    /**
     * Check if a blob with given digest exists in CAS storage.
     */
    Uni<Boolean> exists(Sha256Digest digest);

    /**
     * Retrieve metadata descriptor for a stored blob if it exists.
     */
    Uni<Optional<BlobDescriptor>> getDescriptor(Sha256Digest digest);

    /**
     * Delete blob by SHA-256 digest.
     */
    Uni<Boolean> delete(Sha256Digest digest);
}

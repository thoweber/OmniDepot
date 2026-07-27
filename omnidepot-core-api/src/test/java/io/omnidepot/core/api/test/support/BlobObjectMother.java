package io.omnidepot.core.api.test.support;

import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.Sha256Digest;

import java.time.Instant;
import java.util.UUID;

/**
 * ObjectMother pattern (Martin Fowler) for creating standardized BlobDescriptor instances for testing.
 */
public final class BlobObjectMother {

    private BlobObjectMother() {}

    public static BlobDescriptor createSampleBlob() {
        return createBlobWithDigestAndSize(DigestObjectMother.emptyPayloadDigest(), 1024L);
    }

    public static BlobDescriptor createBlobWithDigestAndSize(Sha256Digest digest, long sizeBytes) {
        return new BlobDescriptor(
                UUID.randomUUID().toString(),
                digest,
                sizeBytes,
                "application/octet-stream",
                "/blobs/sha256/" + digest.hexValue(),
                Instant.now()
        );
    }

    public static BlobDescriptor createOciManifestBlob(Sha256Digest digest, long sizeBytes) {
        return new BlobDescriptor(
                UUID.randomUUID().toString(),
                digest,
                sizeBytes,
                "application/vnd.oci.image.manifest.v1+json",
                "/blobs/sha256/" + digest.hexValue(),
                Instant.now()
        );
    }
}

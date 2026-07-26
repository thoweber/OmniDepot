package io.omnidepot.core.api.storage;

/**
 * Value Object representing a non-negative blob size in bytes.
 * Shares a static final ZERO instance for zero-byte allocations.
 */
public record BlobSize(long bytes) {

    public static final BlobSize ZERO = new BlobSize(0L);

    public BlobSize {
        if (bytes < 0) {
            throw new IllegalArgumentException("Blob size bytes cannot be negative: " + bytes);
        }
    }

    public static BlobSize of(long bytes) {
        return bytes == 0L ? ZERO : new BlobSize(bytes);
    }

    public static BlobSize zero() {
        return ZERO;
    }
}

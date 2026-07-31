package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.Sha256Digest;
import org.jspecify.annotations.Nullable;

/**
 * Value Object representing an algorithm-prefixed OCI Digest string (e.g. "sha256:e3b0c4...").
 * Single Source of Truth for OCI digest formatting, validation, and capacity calculation in repo-format-oci.
 */
public record OciDigest(String value, @Nullable Sha256Digest toSha256) {

    public static final String ALGORITHM_PREFIX = "sha256:";
    public static final int SHA256_HEX_LENGTH = 64;
    public static final int OCI_DIGEST_CAPACITY = ALGORITHM_PREFIX.length() + SHA256_HEX_LENGTH;

    public OciDigest {
        String normalized = value.trim();
        if (!normalized.startsWith(ALGORITHM_PREFIX)) {
            throw new IllegalArgumentException("OCI digest must start with algorithm prefix " + ALGORITHM_PREFIX);
        }
        Sha256Digest parsed = Sha256Digest.of(normalized);
        toSha256 = parsed;
        value = ALGORITHM_PREFIX + parsed.hexValue();
    }

    public static OciDigest fromSha256(Sha256Digest digest) {
        return new OciDigest(ALGORITHM_PREFIX + digest.hexValue(), digest);
    }

    public static OciDigest of(String rawValue) {
        return new OciDigest(rawValue, null);
    }
}

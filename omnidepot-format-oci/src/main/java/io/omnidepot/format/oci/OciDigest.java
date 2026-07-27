package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.Sha256Digest;


/**
 * Value Object representing an algorithm-prefixed OCI Digest string (e.g. "sha256:e3b0c4...").
 * Single Source of Truth for OCI digest formatting and capacity calculation in repo-format-oci.
 */
public record OciDigest(String value) {

    public static final String ALGORITHM_PREFIX = "sha256:";
    public static final int SHA256_HEX_LENGTH = 64;
    public static final int OCI_DIGEST_CAPACITY = ALGORITHM_PREFIX.length() + SHA256_HEX_LENGTH;

    public OciDigest {
        String normalized = value.trim();
        if (!normalized.startsWith(ALGORITHM_PREFIX)) {
            throw new IllegalArgumentException("OCI digest must start with algorithm prefix " + ALGORITHM_PREFIX);
        }
        value = normalized;
    }

    public static OciDigest fromSha256(Sha256Digest digest) {
        String formatted = ALGORITHM_PREFIX +
                digest.hexValue();
        return new OciDigest(formatted);
    }

    public static OciDigest of(String rawValue) {
        return new OciDigest(rawValue);
    }
}

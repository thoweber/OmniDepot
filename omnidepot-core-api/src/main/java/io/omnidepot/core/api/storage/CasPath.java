package io.omnidepot.core.api.storage;


/**
 * Value Object representing a canonical Content-Addressable Storage (CAS) path.
 * Single Source of Truth for CAS storage path formatting and capacity calculation.
 * Format: blobs/sha256/{hex[0..2]}/{hex[2..4]}/{hex}
 */
public record CasPath(String value) {

    public static final String CAS_BLOBS_PREFIX = "blobs/sha256/";
    public static final int SHA256_HEX_LENGTH = 64;
    public static final int CAS_PATH_CAPACITY = CAS_BLOBS_PREFIX.length() + 2 + 1 + 2 + 1 + SHA256_HEX_LENGTH;

    public CasPath {
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("CAS path must not be blank");
        }
        value = normalized;
    }

    public static CasPath fromSha256(Sha256Digest digest) {
        String hex = digest.hexValue();
        String path = CAS_BLOBS_PREFIX +
                hex.substring(0, 2) +
                '/' +
                hex.substring(2, 4) +
                '/' +
                hex;
        return new CasPath(path);
    }

    public static CasPath of(String rawValue) {
        return new CasPath(rawValue);
    }
}

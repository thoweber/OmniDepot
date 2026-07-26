package io.omnidepot.core.api.storage;

import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Value Object representing a validated SHA-256 digest string in lowercase hex format.
 * Optimized for subatomic speed hot-path execution with pre-sized StringBuilder capacity.
 */
public record Sha256Digest(String hexValue) {

    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[a-f0-9]{64}$");
    private static final String OCI_PREFIX = "sha256:";
    private static final String CAS_PREFIX = "blobs/sha256/";

    public Sha256Digest {
        String normalized = isNull(hexValue) ? "" : hexValue.trim().toLowerCase(Locale.ROOT);

        if (normalized.startsWith(OCI_PREFIX)) {
            normalized = normalized.substring(7);
        }

        if (!SHA256_HEX_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid SHA-256 digest format: " + hexValue);
        }

        hexValue = normalized;
    }

    public static Sha256Digest of(String rawValue) {
        return new Sha256Digest(rawValue);
    }

    public String toOciDigestString() {
        return new StringBuilder(71)
                .append(OCI_PREFIX)
                .append(hexValue)
                .toString();
    }

    public String toCasPath() {
        return new StringBuilder(81)
                .append(CAS_PREFIX)
                .append(hexValue, 0, 2)
                .append('/')
                .append(hexValue, 2, 4)
                .append('/')
                .append(hexValue)
                .toString();
    }

    @Override
    public String toString() {
        return hexValue;
    }
}

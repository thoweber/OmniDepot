package io.omnidepot.core.api.storage;

import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Value Object representing a validated SHA-256 digest string in lowercase hex format.
 * Uses normalization-first before validation without Optional instantiation overhead.
 */
public record Sha256Digest(String hexValue) {

    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[a-f0-9]{64}$");

    public Sha256Digest {
        String normalized = isNull(hexValue) ? "" : hexValue.trim().toLowerCase(Locale.ROOT);

        if (normalized.startsWith("sha256:")) {
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
        return "sha256:" + hexValue;
    }

    public String toCasPath() {
        return "blobs/sha256/" + hexValue.substring(0, 2) + "/" + hexValue.substring(2, 4) + "/" + hexValue;
    }

    @Override
    public String toString() {
        return hexValue;
    }
}

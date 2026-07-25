package io.omnidepot.core.api.storage;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a validated SHA-256 digest string in hex format.
 */
public record Sha256Digest(String hexValue) {

    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    public Sha256Digest {
        Objects.requireNonNull(hexValue, "Digest hex value must not be null");
        String trimmed = hexValue.trim().toLowerCase();
        if (trimmed.startsWith("sha256:")) {
            trimmed = trimmed.substring(7);
        }
        if (!SHA256_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid SHA-256 digest format: " + hexValue);
        }
        hexValue = trimmed;
    }

    public static Sha256Digest of(String rawValue) {
        return new Sha256Digest(rawValue);
    }

    public String toOciDigestString() {
        return "sha256:" + hexValue;
    }

    @Override
    public String toString() {
        return hexValue;
    }
}

package io.omnidepot.core.api.storage;

import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Value Object representing a validated SHA-256 digest in lowercase hex format.
 * Encapsulates pure SHA-256 hash invariants without coupling to specific protocol formats.
 */
public record Sha256Digest(String hexValue) {

    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[a-f0-9]{64}$");
    private static final String SHA256_PREFIX = "sha256:";

    public Sha256Digest {
        String normalized = isNull(hexValue) ? "" : hexValue.trim().toLowerCase(Locale.ROOT);

        if (normalized.startsWith(SHA256_PREFIX)) {
            normalized = normalized.substring(SHA256_PREFIX.length());
        }

        if (!SHA256_HEX_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid SHA-256 digest format: " + hexValue);
        }

        hexValue = normalized;
    }

    public static Sha256Digest of(String rawValue) {
        return new Sha256Digest(rawValue);
    }

    public CasPath toCasPath() {
        return CasPath.fromSha256(this);
    }

    @Override
    public String toString() {
        return hexValue;
    }
}

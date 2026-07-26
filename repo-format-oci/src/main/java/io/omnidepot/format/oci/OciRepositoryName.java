package io.omnidepot.format.oci;

import io.omnidepot.core.api.catalog.RepositoryPath;

import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Strongly-typed OCI Repository Name Value Object.
 * Validates OCI V2 repository name components cleanly using possessive quantifiers to eliminate catastrophic backtracking (S5998).
 */
public record OciRepositoryName(String value) implements RepositoryPath {

    private static final Pattern COMPONENT_PATTERN = Pattern.compile("^[a-z0-9]+(?:[._-][a-z0-9]+)*+$");
    private static final int MAX_LENGTH = 255;

    public OciRepositoryName {
        String normalized = isNull(value) ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank() || normalized.length() > MAX_LENGTH || !isValidRepositoryName(normalized)) {
            throw new OciNameInvalidException("Invalid OCI repository name: " + value);
        }

        value = normalized;
    }

    private static boolean isValidRepositoryName(String name) {
        String[] parts = name.split("/", -1);
        for (String part : parts) {
            if (part.isEmpty() || !COMPONENT_PATTERN.matcher(part).matches()) {
                return false;
            }
        }
        return true;
    }

    public static OciRepositoryName of(String rawValue) {
        return new OciRepositoryName(rawValue);
    }
}

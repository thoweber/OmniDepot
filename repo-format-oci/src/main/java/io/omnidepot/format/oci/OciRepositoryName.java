package io.omnidepot.format.oci;

import io.omnidepot.core.api.catalog.RepositoryPath;

import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * Strongly-typed OCI Repository Name Value Object.
 * Implements normalization-first before validation without Optional allocation overhead.
 */
public record OciRepositoryName(String value) implements RepositoryPath {

    private static final Pattern REPO_NAME_PATTERN =
            Pattern.compile("^[a-z0-9]+(?:[._-][a-z0-9]+)*(?:/[a-z0-9]+(?:[._-][a-z0-9]+)*)*$");

    public OciRepositoryName {
        String normalized = isNull(value) ? "" : value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank() || !REPO_NAME_PATTERN.matcher(normalized).matches()) {
            throw new OciNameInvalidException("Invalid OCI repository name: " + value);
        }

        value = normalized;
    }

    public static OciRepositoryName of(String rawValue) {
        return new OciRepositoryName(rawValue);
    }
}

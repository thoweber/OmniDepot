package io.omnidepot.core.api.storage;

import java.util.UUID;

import static java.util.Objects.isNull;

/**
 * Value Object representing a validated upload session identifier.
 */
public record UploadSessionId(String value) {

    public UploadSessionId {
        String normalized = isNull(value) ? "" : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Upload session ID must not be blank");
        }
        value = normalized;
    }

    public static UploadSessionId generate() {
        return new UploadSessionId(UUID.randomUUID().toString());
    }

    public static UploadSessionId of(String rawValue) {
        return new UploadSessionId(rawValue);
    }
}

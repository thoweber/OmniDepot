package io.omnidepot.core.api.upload;

import java.time.Instant;
import java.util.Objects;

/**
 * Record representing persistent resumable chunked upload state.
 */
public record UploadSession(
        String id,
        String repositoryId,
        String uploadToken,
        long bytesReceived,
        Long totalBytes,
        UploadSessionStatus status,
        String providerStateJson,
        Instant createdAt,
        Instant updatedAt
) {
    public UploadSession {
        Objects.requireNonNull(id, "UploadSession id must not be null");
        Objects.requireNonNull(repositoryId, "Repository id must not be null");
        Objects.requireNonNull(uploadToken, "Upload token must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(createdAt, "CreatedAt must not be null");
        Objects.requireNonNull(updatedAt, "UpdatedAt must not be null");
    }
}

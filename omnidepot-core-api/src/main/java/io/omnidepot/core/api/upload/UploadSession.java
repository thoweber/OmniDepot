package io.omnidepot.core.api.upload;

import org.jspecify.annotations.Nullable;

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
        @Nullable Long totalBytes,
        UploadSessionStatus status,
        String providerStateJson,
        byte @Nullable [] sha256PartialState,
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UploadSession that = (UploadSession) o;
        return bytesReceived == that.bytesReceived
                && Objects.equals(id, that.id)
                && Objects.equals(repositoryId, that.repositoryId)
                && Objects.equals(uploadToken, that.uploadToken)
                && Objects.equals(totalBytes, that.totalBytes)
                && status == that.status
                && Objects.equals(providerStateJson, that.providerStateJson)
                && java.util.Arrays.equals(sha256PartialState, that.sha256PartialState)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, repositoryId, uploadToken, bytesReceived, totalBytes, status, providerStateJson, createdAt, updatedAt);
        result = 31 * result + java.util.Arrays.hashCode(sha256PartialState);
        return result;
    }

    @Override
    public String toString() {
        return "UploadSession[" +
                "id=" + id + ", " +
                "repositoryId=" + repositoryId + ", " +
                "uploadToken=" + uploadToken + ", " +
                "bytesReceived=" + bytesReceived + ", " +
                "totalBytes=" + totalBytes + ", " +
                "status=" + status + ", " +
                "providerStateJson=" + providerStateJson + ", " +
                "sha256PartialState=" + java.util.Arrays.toString(sha256PartialState) + ", " +
                "createdAt=" + createdAt + ", " +
                "updatedAt=" + updatedAt + ']';
    }
}

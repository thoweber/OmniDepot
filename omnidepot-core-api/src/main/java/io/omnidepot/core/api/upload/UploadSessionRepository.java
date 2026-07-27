package io.omnidepot.core.api.upload;

import io.smallrye.mutiny.Uni;
import java.util.Optional;

/**
 * Storage SPI for managing resumable chunked upload sessions.
 */
public interface UploadSessionRepository {

    /**
     * Create a new upload session.
     */
    Uni<UploadSession> create(UploadSession session);

    /**
     * Find an active upload session by upload token.
     */
    Uni<Optional<UploadSession>> findByToken(String uploadToken);

    /**
     * Update progress and provider-specific state JSON.
     */
    Uni<UploadSession> updateProgress(String uploadToken, long bytesReceived, String providerStateJson);

    /**
     * Update status of upload session (e.g. COMPLETED, ABORTED).
     */
    Uni<UploadSession> markStatus(String uploadToken, UploadSessionStatus status);

    /**
     * Delete upload session by token.
     */
    Uni<Boolean> deleteByToken(String uploadToken);
}

package io.omnidepot.core.api.test.support;

import io.omnidepot.core.api.upload.UploadSession;
import io.omnidepot.core.api.upload.UploadSessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ObjectMother pattern (Martin Fowler) for creating standardized UploadSession test domain objects using builder.
 */
public final class UploadSessionObjectMother {

    private UploadSessionObjectMother() {}

    public static UploadSession createInitiatedSession(String repositoryId, String uploadToken) {
        Instant now = Instant.now();
        return UploadSession.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(repositoryId)
                .uploadToken(uploadToken)
                .bytesReceived(0L)
                .totalBytes(10485760L) // 10MB
                .status(UploadSessionStatus.INITIATED)
                .providerStateJson("{}")
                .sha256PartialState(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static UploadSession createCompletedSession(String repositoryId, String uploadToken, long bytes) {
        Instant now = Instant.now();
        return UploadSession.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(repositoryId)
                .uploadToken(uploadToken)
                .bytesReceived(bytes)
                .totalBytes(bytes)
                .status(UploadSessionStatus.COMPLETED)
                .providerStateJson("{\"parts\":[1,2]}")
                .sha256PartialState(null)
                .createdAt(now.minusSeconds(60))
                .updatedAt(now)
                .build();
    }
}

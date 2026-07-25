package io.omnidepot.core.api.test.support;

import io.omnidepot.core.api.upload.UploadSession;
import io.omnidepot.core.api.upload.UploadSessionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ObjectMother pattern (Martin Fowler) for creating standardized UploadSession test domain objects.
 */
public final class UploadSessionObjectMother {

    private UploadSessionObjectMother() {}

    public static UploadSession createInitiatedSession(String repositoryId, String uploadToken) {
        Instant now = Instant.now();
        return new UploadSession(
                UUID.randomUUID().toString(),
                repositoryId,
                uploadToken,
                0L,
                10485760L, // 10MB
                UploadSessionStatus.INITIATED,
                "{}",
                now,
                now
        );
    }

    public static UploadSession createCompletedSession(String repositoryId, String uploadToken, long bytes) {
        Instant now = Instant.now();
        return new UploadSession(
                UUID.randomUUID().toString(),
                repositoryId,
                uploadToken,
                bytes,
                bytes,
                UploadSessionStatus.COMPLETED,
                "{\"parts\":[1,2]}",
                now.minusSeconds(60),
                now
        );
    }
}

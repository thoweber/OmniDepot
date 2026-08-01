package io.omnidepot.infra.db.upload;

import io.omnidepot.core.api.upload.UploadSession;
import io.omnidepot.core.api.upload.UploadSessionRepository;
import io.omnidepot.core.api.upload.UploadSessionStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class UploadSessionRepositoryTest {

    @Inject
    UploadSessionRepository uploadSessionRepository;

    @Test
    @DisplayName("Given new upload session - when creating - then persists in DB and is retrievable by token")
    void shouldCreateAndRetrieveUploadSession() {
        String repoId = UUID.randomUUID().toString();
        String token = "test-token-" + UUID.randomUUID();

        UploadSession newSession = createTestSession(repoId, token);

        UploadSession created = uploadSessionRepository.create(newSession).await().indefinitely();
        assertThat(created.id()).isEqualTo(newSession.id());
        assertThat(created.uploadToken()).isEqualTo(token);

        Optional<UploadSession> found = uploadSessionRepository.findByToken(token).await().indefinitely();
        assertThat(found).isPresent();
        assertThat(found.get().bytesReceived()).isZero();
        assertThat(found.get().status()).isEqualTo(UploadSessionStatus.INITIATED);
    }

    @Test
    @DisplayName("Given active upload session - when updating progress - then byte count and partial state are updated")
    void shouldUpdateProgressAndPartialState() {
        String repoId = UUID.randomUUID().toString();
        String token = "progress-token-" + UUID.randomUUID();

        UploadSession session = createTestSession(repoId, token);
        uploadSessionRepository.create(session).await().indefinitely();

        byte[] partialState = new byte[]{1, 2, 3, 4, 5};
        UploadSession updated = uploadSessionRepository.updateProgress(token, 5242880L, "{\"parts\":[1]}", partialState).await().indefinitely();

        assertThat(updated.bytesReceived()).isEqualTo(5242880L);
        assertThat(updated.sha256PartialState()).isEqualTo(partialState);
        assertThat(updated.providerStateJson()).isEqualTo("{\"parts\":[1]}");

        Optional<UploadSession> reloaded = uploadSessionRepository.findByToken(token).await().indefinitely();
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().sha256PartialState()).isEqualTo(partialState);
    }

    @Test
    @DisplayName("Given active upload session - when marking status and deleting - then status updates and deletion succeeds")
    void shouldMarkStatusAndDeleteSession() {
        String repoId = UUID.randomUUID().toString();
        String token = "lifecycle-token-" + UUID.randomUUID();

        UploadSession session = createTestSession(repoId, token);
        uploadSessionRepository.create(session).await().indefinitely();

        UploadSession completed = uploadSessionRepository.markStatus(token, UploadSessionStatus.COMPLETED).await().indefinitely();
        assertThat(completed.status()).isEqualTo(UploadSessionStatus.COMPLETED);

        Boolean deleted = uploadSessionRepository.deleteByToken(token).await().indefinitely();
        assertThat(deleted).isTrue();

        Optional<UploadSession> afterDelete = uploadSessionRepository.findByToken(token).await().indefinitely();
        assertThat(afterDelete).isEmpty();
    }

    @Test
    @DisplayName("Given non-existent token - when updating progress or marking status - then throws IllegalArgumentException")
    void shouldHandleNonExistentTokenOperations() {
        String invalidToken = "non-existent-token-" + UUID.randomUUID();

        assertThat(uploadSessionRepository.deleteByToken(invalidToken).await().indefinitely()).isFalse();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> uploadSessionRepository.updateProgress(invalidToken, 100L, "{}", null).await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> uploadSessionRepository.markStatus(invalidToken, UploadSessionStatus.COMPLETED).await().indefinitely())
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static UploadSession createTestSession(String repoId, String uploadToken) {
        Instant now = Instant.now();
        return new UploadSession(
                UUID.randomUUID().toString(),
                repoId,
                uploadToken,
                0L,
                10485760L,
                UploadSessionStatus.INITIATED,
                "{}",
                null,
                now,
                now
        );
    }
}

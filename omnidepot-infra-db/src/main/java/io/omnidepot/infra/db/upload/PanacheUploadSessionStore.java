package io.omnidepot.infra.db.upload;

import io.omnidepot.core.api.upload.UploadSession;
import io.omnidepot.core.api.upload.UploadSessionRepository;
import io.omnidepot.core.api.upload.UploadSessionStatus;
import io.omnidepot.infra.db.RepositoryEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;

import static java.util.Objects.isNull;

@ApplicationScoped
@SuppressWarnings("java:S3252")
class PanacheUploadSessionStore implements UploadSessionRepository {

    @Override
    public Uni<UploadSession> create(UploadSession session) {
        return Uni.createFrom().item(() ->
                QuarkusTransaction.requiringNew().call(() -> doCreate(session))
        );
    }

    private UploadSession doCreate(UploadSession session) {
        RepositoryEntity repo = RepositoryEntity.<RepositoryEntity>findById(session.repositoryId());
        if (isNull(repo)) {
            repo = RepositoryEntity.<RepositoryEntity>find("name", session.repositoryId()).firstResult();
        }
        if (isNull(repo)) {
            repo = new RepositoryEntity();
            repo.id = session.repositoryId();
            repo.name = session.repositoryId();
            repo.format = "oci";
            repo.isVirtual = false;
            repo.createdAt = Instant.now();
            repo.updatedAt = Instant.now();
            repo.persist();
        }

        UploadSessionEntity entity = new UploadSessionEntity();
        entity.id = session.id();
        entity.repositoryId = repo.id;
        entity.uploadToken = session.uploadToken();
        entity.bytesReceived = session.bytesReceived();
        entity.totalBytes = session.totalBytes();
        entity.status = session.status().name();
        entity.providerState = session.providerStateJson();
        entity.sha256PartialState = session.sha256PartialState();
        entity.createdAt = session.createdAt();
        entity.updatedAt = session.updatedAt();
        entity.persist();

        return toDomain(entity);
    }

    @Override
    public Uni<Optional<UploadSession>> findByToken(String uploadToken) {
        return Uni.createFrom().item(() ->
                QuarkusTransaction.requiringNew().call(() -> doFindByToken(uploadToken))
        );
    }

    private Optional<UploadSession> doFindByToken(String uploadToken) {
        UploadSessionEntity entity = UploadSessionEntity.<UploadSessionEntity>find("uploadToken", uploadToken).firstResult();
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Uni<UploadSession> updateProgress(String uploadToken, long bytesReceived, String providerStateJson, byte @Nullable [] sha256PartialState) {
        return Uni.createFrom().item(() ->
                QuarkusTransaction.requiringNew().call(() -> doUpdateProgress(uploadToken, bytesReceived, providerStateJson, sha256PartialState))
        );
    }

    private UploadSession doUpdateProgress(String uploadToken, long bytesReceived, String providerStateJson, byte @Nullable [] sha256PartialState) {
        UploadSessionEntity entity = UploadSessionEntity.<UploadSessionEntity>find("uploadToken", uploadToken).firstResult();
        if (isNull(entity)) {
            throw new IllegalArgumentException("Upload session not found for token: " + uploadToken);
        }
        entity.bytesReceived = bytesReceived;
        entity.providerState = providerStateJson;
        if (sha256PartialState != null) {
            entity.sha256PartialState = sha256PartialState;
        }
        entity.updatedAt = Instant.now();
        entity.persist();

        return toDomain(entity);
    }

    @Override
    public Uni<UploadSession> markStatus(String uploadToken, UploadSessionStatus status) {
        return Uni.createFrom().item(() ->
                QuarkusTransaction.requiringNew().call(() -> doMarkStatus(uploadToken, status))
        );
    }

    private UploadSession doMarkStatus(String uploadToken, UploadSessionStatus status) {
        UploadSessionEntity entity = UploadSessionEntity.<UploadSessionEntity>find("uploadToken", uploadToken).firstResult();
        if (isNull(entity)) {
            throw new IllegalArgumentException("Upload session not found for token: " + uploadToken);
        }
        entity.status = status.name();
        entity.updatedAt = Instant.now();
        entity.persist();

        return toDomain(entity);
    }

    @Override
    public Uni<Boolean> deleteByToken(String uploadToken) {
        return Uni.createFrom().item(() ->
                QuarkusTransaction.requiringNew().call(() -> doDeleteByToken(uploadToken))
        );
    }

    private boolean doDeleteByToken(String uploadToken) {
        long deletedCount = UploadSessionEntity.delete("uploadToken", uploadToken);
        return deletedCount > 0;
    }

    private UploadSession toDomain(UploadSessionEntity entity) {
        return new UploadSession(
                entity.id,
                entity.repositoryId,
                entity.uploadToken,
                entity.bytesReceived,
                entity.totalBytes,
                UploadSessionStatus.valueOf(entity.status),
                entity.providerState != null ? entity.providerState : "{}",
                entity.sha256PartialState,
                entity.createdAt,
                entity.updatedAt
        );
    }
}

package io.omnidepot.infra.db.upload;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "upload_sessions")
class UploadSessionEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    public String id = "";

    @Column(name = "repository_id", length = 36, nullable = false)
    public String repositoryId = "";

    @Column(name = "upload_token", length = 255, nullable = false, unique = true)
    public String uploadToken = "";

    @Column(name = "bytes_received", nullable = false)
    public long bytesReceived;

    @Column(name = "total_bytes")
    public @Nullable Long totalBytes;

    @Column(name = "status", length = 32, nullable = false)
    public String status = "";

    @Column(name = "provider_state")
    public @Nullable String providerState;

    @Column(name = "sha256_partial_state")
    public byte @Nullable [] sha256PartialState;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}

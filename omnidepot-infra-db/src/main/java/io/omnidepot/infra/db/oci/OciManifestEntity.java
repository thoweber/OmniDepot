package io.omnidepot.infra.db.oci;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "oci_manifests")
class OciManifestEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    public String id = "";

    @Column(name = "repository_id", length = 36, nullable = false)
    public String repositoryId = "";

    @Column(name = "digest", length = 71, nullable = false)
    public String digest = "";

    @Column(name = "media_type", length = 255, nullable = false)
    public String mediaType = "";

    @Column(name = "size_bytes", nullable = false)
    public long sizeBytes;

    @Column(name = "payload", nullable = false)
    public String payload = "";

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}

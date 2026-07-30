package io.omnidepot.infra.db.oci;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "oci_tags")
class OciTagEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    public String id = "";

    @Column(name = "repository_id", length = 36, nullable = false)
    public String repositoryId = "";

    @Column(name = "tag_name", length = 128, nullable = false)
    public String tagName = "";

    @Column(name = "manifest_id", length = 36, nullable = false)
    public String manifestId = "";

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}

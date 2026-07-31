package io.omnidepot.infra.db.oci;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "repositories")
class RepositoryEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    public String id = "";

    @Column(name = "name", length = 255, nullable = false, unique = true)
    public String name = "";

    @Column(name = "format", length = 64, nullable = false)
    public String format = "oci";

    @Column(name = "is_virtual", nullable = false)
    public boolean isVirtual = false;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    @Column(name = "attributes")
    public @Nullable String attributes;
}

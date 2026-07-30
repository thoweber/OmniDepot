package io.omnidepot.infra.db.oci;

import io.omnidepot.core.api.oci.ManifestStore;
import io.omnidepot.core.api.oci.StoredManifestRecord;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
class PanacheManifestStore implements ManifestStore {

    private static final Logger LOG = Logger.getLogger(PanacheManifestStore.class);

    @Override
    public Uni<StoredManifestRecord> saveManifest(String repositoryName, String reference, String mediaType, String payload) {
        return Uni.createFrom().item(() ->
                QuarkusTransaction.requiringNew().call(() -> doSaveManifest(repositoryName, reference, mediaType, payload))
        );
    }

    private StoredManifestRecord doSaveManifest(String repositoryName, String reference, String mediaType, String payload) {
        RepositoryEntity repo = RepositoryEntity.<RepositoryEntity>find("name", repositoryName).firstResult();
        if (repo == null) {
            repo = new RepositoryEntity();
            repo.id = UUID.randomUUID().toString();
            repo.name = repositoryName;
            repo.format = "oci";
            repo.isVirtual = false;
            repo.createdAt = Instant.now();
            repo.updatedAt = Instant.now();
            repo.persist();
        }

        String computedHex = calculateSha256Hex(payload.getBytes(StandardCharsets.UTF_8));
        String canonicalDigestStr = "sha256:" + computedHex;

        OciManifestEntity manifest = OciManifestEntity.<OciManifestEntity>find("repositoryId = ?1 and digest = ?2", repo.id, canonicalDigestStr).firstResult();
        if (manifest == null) {
            manifest = new OciManifestEntity();
            manifest.id = UUID.randomUUID().toString();
            manifest.repositoryId = repo.id;
            manifest.digest = canonicalDigestStr;
            manifest.mediaType = mediaType;
            manifest.sizeBytes = payload.getBytes(StandardCharsets.UTF_8).length;
            manifest.payload = payload;
            manifest.createdAt = Instant.now();
            manifest.persist();
        }

        if (!reference.startsWith("sha256:")) {
            OciTagEntity tag = OciTagEntity.<OciTagEntity>find("repositoryId = ?1 and tagName = ?2", repo.id, reference).firstResult();
            if (tag == null) {
                tag = new OciTagEntity();
                tag.id = UUID.randomUUID().toString();
                tag.repositoryId = repo.id;
                tag.tagName = reference;
            }
            tag.manifestId = manifest.id;
            tag.updatedAt = Instant.now();
            tag.persist();
        }

        return toRecord(repositoryName, manifest);
    }

    @Override
    public Uni<Optional<StoredManifestRecord>> findManifest(String repositoryName, String reference) {
        return Uni.createFrom().item(() ->
                QuarkusTransaction.requiringNew().call(() -> doFindManifest(repositoryName, reference))
        );
    }

    private Optional<StoredManifestRecord> doFindManifest(String repositoryName, String reference) {
        RepositoryEntity repo = RepositoryEntity.<RepositoryEntity>find("name", repositoryName).firstResult();
        if (repo == null) {
            return Optional.empty();
        }

        OciManifestEntity manifest;
        if (reference.startsWith("sha256:")) {
            manifest = OciManifestEntity.<OciManifestEntity>find("repositoryId = ?1 and digest = ?2", repo.id, reference).firstResult();
        } else {
            OciTagEntity tag = OciTagEntity.<OciTagEntity>find("repositoryId = ?1 and tagName = ?2", repo.id, reference).firstResult();
            if (tag == null) {
                return Optional.empty();
            }
            manifest = OciManifestEntity.findById(tag.manifestId);
        }

        if (manifest == null) {
            return Optional.empty();
        }

        return Optional.of(toRecord(repositoryName, manifest));
    }

    @Override
    public Uni<Boolean> manifestExists(String repositoryName, String reference) {
        return findManifest(repositoryName, reference).map(Optional::isPresent);
    }

    private StoredManifestRecord toRecord(String repoName, OciManifestEntity entity) {
        String hex = entity.digest.startsWith("sha256:") ? entity.digest.substring(7) : entity.digest;
        return new StoredManifestRecord(
                entity.id,
                repoName,
                Sha256Digest.of(hex),
                entity.mediaType,
                entity.sizeBytes,
                entity.payload,
                entity.createdAt
        );
    }

    private static String calculateSha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}

package io.omnidepot.infra.db.oci;

import io.omnidepot.core.api.oci.ManifestStore;
import io.omnidepot.core.api.oci.StoredManifestRecord;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PanacheManifestStoreTest {

    private static final String PAYLOAD = """
            {
              "schemaVersion": 2,
              "mediaType": "application/vnd.oci.image.manifest.v1+json",
              "config": {
                "mediaType": "application/vnd.oci.image.config.v1+json",
                "size": 1234,
                "digest": "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
              },
              "layers": []
            }
            """;

    @Inject
    ManifestStore manifestStore;

    @Test
    @DisplayName("Given valid manifest payload - when saving and querying by tag and digest - then correct StoredManifestRecord is returned")
    void shouldSaveAndRetrieveManifestByTagAndDigest() {
        StoredManifestRecord saved = manifestStore.saveManifest(
                "library/alpine",
                "3.18",
                "application/vnd.oci.image.manifest.v1+json",
                PAYLOAD
        ).await().indefinitely();

        assertThat(saved).isNotNull();
        assertThat(saved.repositoryName()).isEqualTo("library/alpine");
        assertThat(saved.mediaType()).isEqualTo("application/vnd.oci.image.manifest.v1+json");
        assertThat(saved.payload()).isEqualTo(PAYLOAD);

        String digestStr = "sha256:" + saved.digest().hexValue();

        // Query by tag
        Optional<StoredManifestRecord> byTag = manifestStore.findManifest("library/alpine", "3.18")
                .await().indefinitely();
        assertThat(byTag).isPresent();
        assertThat(byTag.get().digest().hexValue()).isEqualTo(saved.digest().hexValue());

        // Query by digest
        Optional<StoredManifestRecord> byDigest = manifestStore.findManifest("library/alpine", digestStr)
                .await().indefinitely();
        assertThat(byDigest).isPresent();
        assertThat(byDigest.get().payload()).isEqualTo(PAYLOAD);

        // Verify existence checks
        Boolean tagExists = manifestStore.manifestExists("library/alpine", "3.18").await().indefinitely();
        Boolean digestExists = manifestStore.manifestExists("library/alpine", digestStr).await().indefinitely();
        Boolean missingExists = manifestStore.manifestExists("library/alpine", "nonexistent").await().indefinitely();

        assertThat(tagExists).isTrue();
        assertThat(digestExists).isTrue();
        assertThat(missingExists).isFalse();
    }

    @Test
    @DisplayName("Given existing tag - when re-tagging with new manifest - then tag updates to point to new manifest digest")
    void shouldUpdateTagToNewManifest() {
        String payload1 = PAYLOAD;
        String payload2 = PAYLOAD.replace("1234", "5678");

        StoredManifestRecord v1 = manifestStore.saveManifest("my-org/app", "latest", "application/vnd.oci.image.manifest.v1+json", payload1)
                .await().indefinitely();
        StoredManifestRecord v2 = manifestStore.saveManifest("my-org/app", "latest", "application/vnd.oci.image.manifest.v1+json", payload2)
                .await().indefinitely();

        assertThat(v1.digest().hexValue()).isNotEqualTo(v2.digest().hexValue());

        Optional<StoredManifestRecord> currentLatest = manifestStore.findManifest("my-org/app", "latest")
                .await().indefinitely();
        assertThat(currentLatest).isPresent();
        assertThat(currentLatest.get().digest().hexValue()).isEqualTo(v2.digest().hexValue());
    }

    @Test
    @DisplayName("Given saving directly with a digest reference - when saving manifest - then manifest is saved without creating a tag")
    void shouldSaveManifestWithDigestReference() {
        StoredManifestRecord saved = manifestStore.saveManifest(
                "library/ubuntu",
                "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "application/vnd.oci.image.manifest.v1+json",
                PAYLOAD
        ).await().indefinitely();

        assertThat(saved).isNotNull();
        String digestStr = "sha256:" + saved.digest().hexValue();

        Optional<StoredManifestRecord> found = manifestStore.findManifest("library/ubuntu", digestStr)
                .await().indefinitely();
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Given nonexistent repository or missing reference - when finding manifest - then Optional.empty is returned")
    void shouldReturnEmptyForNonexistentRepoOrReference() {
        Optional<StoredManifestRecord> noRepo = manifestStore.findManifest("nonexistent/repo", "latest")
                .await().indefinitely();
        assertThat(noRepo).isEmpty();

        // Create repo first
        manifestStore.saveManifest("existing/repo", "1.0", "application/vnd.oci.image.manifest.v1+json", PAYLOAD)
                .await().indefinitely();

        Optional<StoredManifestRecord> noTag = manifestStore.findManifest("existing/repo", "missing-tag")
                .await().indefinitely();
        assertThat(noTag).isEmpty();

        Optional<StoredManifestRecord> noDigest = manifestStore.findManifest("existing/repo", "sha256:0000000000000000000000000000000000000000000000000000000000000000")
                .await().indefinitely();
        assertThat(noDigest).isEmpty();
    }
}

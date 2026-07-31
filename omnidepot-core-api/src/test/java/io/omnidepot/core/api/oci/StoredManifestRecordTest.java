package io.omnidepot.core.api.oci;

import io.omnidepot.core.api.storage.Sha256Digest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StoredManifestRecordTest {

    @Test
    @DisplayName("Given valid parameters - when instantiating StoredManifestRecord - then all properties are correctly exposed")
    void shouldConstructStoredManifestRecord() {
        String id = "manifest-123";
        String repoName = "library/alpine";
        Sha256Digest digest = Sha256Digest.of("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        String mediaType = "application/vnd.oci.image.manifest.v1+json";
        long sizeBytes = 512;
        String payload = "{}";
        Instant createdAt = Instant.now();

        StoredManifestRecord storedRecord = new StoredManifestRecord(
                id,
                repoName,
                digest,
                mediaType,
                sizeBytes,
                payload,
                createdAt
        );

        assertThat(storedRecord.id()).isEqualTo(id);
        assertThat(storedRecord.repositoryName()).isEqualTo(repoName);
        assertThat(storedRecord.digest()).isEqualTo(digest);
        assertThat(storedRecord.mediaType()).isEqualTo(mediaType);
        assertThat(storedRecord.sizeBytes()).isEqualTo(sizeBytes);
        assertThat(storedRecord.payload()).isEqualTo(payload);
        assertThat(storedRecord.createdAt()).isEqualTo(createdAt);
    }
}

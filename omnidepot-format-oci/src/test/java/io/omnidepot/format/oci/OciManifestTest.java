package io.omnidepot.format.oci;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for OCI Image Manifest V2 Schema 2 record models and canonical SHA-256 hashing.
 */
class OciManifestTest {

    private static final String SAMPLE_CONFIG_DIGEST = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String SAMPLE_LAYER_DIGEST = "sha256:a5be02727d5be41f79f22c08d9073d965e6488339b647d431f456d953fb3033f";

    @Test
    @DisplayName("Should create valid OCI manifest record and serialize to spec JSON")
    void shouldCreateValidOciManifestRecord() {
        OciDescriptor config = new OciDescriptor(
                "application/vnd.oci.image.config.v1+json",
                7023L,
                SAMPLE_CONFIG_DIGEST
        );

        OciDescriptor layer = new OciDescriptor(
                "application/vnd.oci.image.layer.v1.tar+gzip",
                32654L,
                SAMPLE_LAYER_DIGEST
        );

        OciManifestRecord manifest = new OciManifestRecord(
                2,
                "application/vnd.oci.image.manifest.v1+json",
                config,
                List.of(layer)
        );

        assertThat(manifest.schemaVersion()).isEqualTo(2);
        assertThat(manifest.mediaType()).isEqualTo("application/vnd.oci.image.manifest.v1+json");
        assertThat(manifest.config().digest()).isEqualTo(SAMPLE_CONFIG_DIGEST);
        assertThat(manifest.layers()).hasSize(1);
        assertThat(manifest.layers().getFirst().digest()).isEqualTo(SAMPLE_LAYER_DIGEST);
    }

    @Test
    @DisplayName("Should compute canonical SHA-256 digest for manifest JSON payload")
    void shouldComputeCanonicalSha256Digest() {
        String jsonPayload = """
                {
                  "schemaVersion": 2,
                  "mediaType": "application/vnd.oci.image.manifest.v1+json",
                  "config": {
                    "mediaType": "application/vnd.oci.image.config.v1+json",
                    "size": 7023,
                    "digest": "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                  },
                  "layers": [
                    {
                      "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
                      "size": 32654,
                      "digest": "sha256:a5be02727d5be41f79f22c08d9073d965e6488339b647d431f456d953fb3033f"
                    }
                  ]
                }
                """;

        OciDigest calculatedDigest = OciManifestRecord.calculateDigest(jsonPayload.getBytes());
        assertThat(calculatedDigest.value()).startsWith("sha256:");
        assertThat(calculatedDigest.value()).hasSize(71);
    }

    @Test
    @DisplayName("Should throw OciManifestInvalidException when parsing malformed JSON manifest")
    void shouldThrowExceptionOnMalformedJson() {
        String malformedJson = "{ \"schemaVersion\": 2, invalid_json }";

        assertThatThrownBy(() -> OciManifestRecord.fromJson(malformedJson))
                .isInstanceOf(OciManifestInvalidException.class)
                .hasMessageContaining("Failed to parse OCI Image Manifest JSON");
    }
}

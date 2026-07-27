package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Component test verifying OCI Manifest Ingestion REST endpoints and CAS layer existence guard (Sub-Issue #8).
 */
class OciManifestIngestionCT {

    private OciDistributionResource resource;
    private InMemoryBlobStore stubBlobStore;

    private static final String CONFIG_DIGEST = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String LAYER_DIGEST = "a5be02727d5be41f79f22c08d9073d965e6488339b647d431f456d953fb3033f";

    private static final String VALID_MANIFEST_JSON = """
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

    @BeforeEach
    void setUp() {
        stubBlobStore = new InMemoryBlobStore();
        resource = new OciDistributionResource(stubBlobStore);
    }

    @Test
    @DisplayName("Should ingest manifest successfully when config and layer blobs exist in CAS")
    void shouldIngestManifestWhenBlobsExist() {
        stubBlobStore.addBlob(Sha256Digest.of(CONFIG_DIGEST), "config-data".getBytes());
        stubBlobStore.addBlob(Sha256Digest.of(LAYER_DIGEST), "layer-data".getBytes());

        Response response = resource.putManifest("my-org/alpine", "1.0.0", VALID_MANIFEST_JSON);

        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(response.getHeaderString("Location")).contains("/v2/my-org/alpine/manifests/1.0.0");
        assertThat(response.getHeaderString("Docker-Content-Digest")).startsWith("sha256:");
    }

    @Test
    @DisplayName("Should throw OciBlobUnknownException when referenced layer blob is missing from CAS")
    void shouldThrowExceptionWhenLayerMissingFromCas() {
        stubBlobStore.addBlob(Sha256Digest.of(CONFIG_DIGEST), "config-data".getBytes());
        // Layer blob is omitted from CAS

        assertThatThrownBy(() -> resource.putManifest("my-org/alpine", "1.0.0", VALID_MANIFEST_JSON))
                .isInstanceOf(OciBlobUnknownException.class)
                .hasMessageContaining(LAYER_DIGEST);
    }

    @Test
    @DisplayName("Should retrieve stored manifest via GET endpoint")
    void shouldGetStoredManifest() {
        stubBlobStore.addBlob(Sha256Digest.of(CONFIG_DIGEST), "config-data".getBytes());
        stubBlobStore.addBlob(Sha256Digest.of(LAYER_DIGEST), "layer-data".getBytes());

        resource.putManifest("my-org/alpine", "latest", VALID_MANIFEST_JSON);

        Response response = resource.getManifest("my-org/alpine", "latest");

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/vnd.oci.image.manifest.v1+json");
        assertThat(response.getHeaderString("Docker-Content-Digest")).startsWith("sha256:");
        assertThat(response.getEntity()).isEqualTo(VALID_MANIFEST_JSON);
    }

    private static class InMemoryBlobStore implements BlobStore {
        private final Map<Sha256Digest, byte[]> storage = new HashMap<>();

        void addBlob(Sha256Digest digest, byte[] data) {
            storage.put(digest, data);
        }

        @Override
        public Uni<BlobDescriptor> put(Sha256Digest digest, String mediaType, InputStream dataSupplier, long sizeBytes) {
            return Uni.createFrom().item(new BlobDescriptor(digest.hexValue(), digest, sizeBytes, mediaType, digest.hexValue(), Instant.now()));
        }

        @Override
        public Uni<InputStream> openStream(Sha256Digest digest) {
            byte[] bytes = storage.get(digest);
            return Uni.createFrom().item(new ByteArrayInputStream(bytes != null ? bytes : new byte[0]));
        }

        @Override
        public Uni<Boolean> exists(Sha256Digest digest) {
            return Uni.createFrom().item(storage.containsKey(digest));
        }

        @Override
        public Uni<Optional<BlobDescriptor>> getDescriptor(Sha256Digest digest) {
            byte[] bytes = storage.get(digest);
            return Uni.createFrom().item(Optional.ofNullable(bytes)
                    .map(b -> new BlobDescriptor(digest.hexValue(), digest, b.length, "application/octet-stream", digest.hexValue(), Instant.now())));
        }

        @Override
        public Uni<Boolean> delete(Sha256Digest digest) {
            return Uni.createFrom().item(storage.remove(digest) != null);
        }
    }
}

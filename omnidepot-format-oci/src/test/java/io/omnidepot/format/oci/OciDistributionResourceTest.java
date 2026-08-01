package io.omnidepot.format.oci;

import io.omnidepot.core.api.oci.ManifestStore;
import io.omnidepot.core.api.oci.StoredManifestRecord;
import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.omnidepot.core.api.upload.UploadSession;
import io.omnidepot.core.api.upload.UploadSessionRepository;
import io.omnidepot.core.api.upload.UploadSessionStatus;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OciDistributionResourceTest {

    private OciDistributionResource resource;
    private InMemoryUploadSessionRepository sessionRepo;

    @BeforeEach
    void setUp() {
        sessionRepo = new InMemoryUploadSessionRepository();
        resource = new OciDistributionResource(new StubBlobStore(), new StubManifestStore(), sessionRepo);
    }

    @Test
    @DisplayName("Given OCI client ping - when checking API version - then 200 OK with registry/2.0 header is returned")
    void shouldReturnApiVersionHeader() {
        Response response = resource.checkApiVersion();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString(OciHttpHeader.DOCKER_DISTRIBUTION_API_VERSION.value())).isEqualTo("registry/2.0");
    }

    @Test
    @DisplayName("Given blob upload initiation - when POST /v2/{name}/blobs/uploads is invoked - then 202 Accepted with upload location is returned")
    void shouldInitiateBlobUploadSession() {
        Response response = resource.handleBlobUploadOrMount("test-repo", null, null);

        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(response.getHeaderString(HttpHeaders.LOCATION)).startsWith("/v2/test-repo/blobs/uploads/");
        assertThat(response.getHeaderString(OciHttpHeader.RANGE.value())).isEqualTo("0-0");
    }

    @Test
    @DisplayName("Given chunk data - when PATCH /v2/{name}/blobs/uploads/{sessionId} is invoked - then progress and digest accumulator update")
    void shouldHandleChunkUploadPatch() {
        Response initResponse = resource.handleBlobUploadOrMount("test-repo", null, null);
        String location = initResponse.getHeaderString(HttpHeaders.LOCATION);
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        byte[] chunk1 = "Hello OCI Chunk 1! ".getBytes(StandardCharsets.UTF_8);
        Response patchResponse = resource.handleChunkUpload("test-repo", sessionId, chunk1);

        assertThat(patchResponse.getStatus()).isEqualTo(202);
        assertThat(patchResponse.getHeaderString(OciHttpHeader.RANGE.value())).isEqualTo("0-" + (chunk1.length - 1));

        Response statusResponse = resource.getUploadSessionStatus("test-repo", sessionId);
        assertThat(statusResponse.getStatus()).isEqualTo(204);
        assertThat(statusResponse.getHeaderString(OciHttpHeader.RANGE.value())).isEqualTo("0-" + (chunk1.length - 1));
    }

    @Test
    @DisplayName("Given active session - when cancelled via DELETE - then session is removed")
    void shouldCancelUploadSession() {
        Response initResponse = resource.handleBlobUploadOrMount("test-repo", null, null);
        String location = initResponse.getHeaderString(HttpHeaders.LOCATION);
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        Response deleteResponse = resource.cancelUploadSession("test-repo", sessionId);
        assertThat(deleteResponse.getStatus()).isEqualTo(204);

        assertThatThrownBy(() -> resource.getUploadSessionStatus("test-repo", sessionId))
                .isInstanceOf(OciBlobUploadUnknownException.class);
    }

    @Test
    @DisplayName("Given chunked upload - when finalized with matching digest - then 201 Created is returned")
    void shouldFinalizeChunkedBlobUploadWithValidDigest() throws Exception {
        Response initResponse = resource.handleBlobUploadOrMount("test-repo", null, null);
        String location = initResponse.getHeaderString(HttpHeaders.LOCATION);
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        byte[] chunk1 = "Chunk Alpha ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "Chunk Beta".getBytes(StandardCharsets.UTF_8);

        resource.handleChunkUpload("test-repo", sessionId, chunk1);
        resource.handleChunkUpload("test-repo", sessionId, chunk2);

        byte[] fullContent = "Chunk Alpha Chunk Beta".getBytes(StandardCharsets.UTF_8);
        String expectedDigestStr = "sha256:" + calculateSha256Hex(fullContent);

        Response finalizeResponse = resource.finalizeUpload("test-repo", sessionId, expectedDigestStr, null);
        assertThat(finalizeResponse.getStatus()).isEqualTo(201);
        assertThat(finalizeResponse.getHeaderString(OciHttpHeader.DOCKER_CONTENT_DIGEST.value())).isEqualTo(expectedDigestStr);
    }

    @Test
    @DisplayName("Given chunked upload - when finalized with mismatching digest - then OciDigestInvalidException is thrown")
    void shouldRejectFinalizeWithMismatchingDigest() {
        Response initResponse = resource.handleBlobUploadOrMount("test-repo", null, null);
        String location = initResponse.getHeaderString(HttpHeaders.LOCATION);
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        byte[] chunk1 = "Actual Content".getBytes(StandardCharsets.UTF_8);
        resource.handleChunkUpload("test-repo", sessionId, chunk1);

        String wrongDigestStr = "sha256:0000000000000000000000000000000000000000000000000000000000000000";

        assertThatThrownBy(() -> resource.finalizeUpload("test-repo", sessionId, wrongDigestStr, null))
                .isInstanceOf(OciDigestInvalidException.class)
                .hasMessageContaining("Digest mismatch");
    }

    private static String calculateSha256Hex(byte[] input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input);
        return HexFormat.of().formatHex(hash);
    }

    private static class InMemoryUploadSessionRepository implements UploadSessionRepository {
        private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

        @Override
        public Uni<UploadSession> create(UploadSession session) {
            sessions.put(session.uploadToken(), session);
            return Uni.createFrom().item(session);
        }

        @Override
        public Uni<Optional<UploadSession>> findByToken(String uploadToken) {
            return Uni.createFrom().item(Optional.ofNullable(sessions.get(uploadToken)));
        }

        @Override
        public Uni<UploadSession> updateProgress(String uploadToken, long bytesReceived, String providerStateJson, byte @Nullable [] sha256PartialState) {
            UploadSession current = sessions.get(uploadToken);
            if (current == null) {
                return Uni.createFrom().failure(new IllegalArgumentException("Session not found"));
            }
            UploadSession updated = current.toBuilder()
                    .bytesReceived(bytesReceived)
                    .providerStateJson(providerStateJson)
                    .sha256PartialState(sha256PartialState != null ? sha256PartialState : current.sha256PartialState())
                    .updatedAt(Instant.now())
                    .build();
            sessions.put(uploadToken, updated);
            return Uni.createFrom().item(updated);
        }

        @Override
        public Uni<UploadSession> markStatus(String uploadToken, UploadSessionStatus status) {
            UploadSession current = sessions.get(uploadToken);
            if (current == null) {
                return Uni.createFrom().failure(new IllegalArgumentException("Session not found"));
            }
            UploadSession updated = current.toBuilder()
                    .status(status)
                    .updatedAt(Instant.now())
                    .build();
            sessions.put(uploadToken, updated);
            return Uni.createFrom().item(updated);
        }

        @Override
        public Uni<Boolean> deleteByToken(String uploadToken) {
            UploadSession removed = sessions.remove(uploadToken);
            return Uni.createFrom().item(removed != null);
        }
    }

    private static class StubBlobStore implements BlobStore {
        @Override
        public Uni<BlobDescriptor> put(Sha256Digest digest, String mediaType, InputStream dataSupplier, long sizeBytes) {
            return Uni.createFrom().item(new BlobDescriptor(digest.hexValue(), digest, sizeBytes, mediaType, digest.hexValue(), Instant.now()));
        }

        @Override
        public Uni<InputStream> openStream(Sha256Digest digest) {
            return Uni.createFrom().item(new ByteArrayInputStream(new byte[0]));
        }

        @Override
        public Uni<Boolean> exists(Sha256Digest digest) {
            return Uni.createFrom().item(true);
        }

        @Override
        public Uni<Optional<BlobDescriptor>> getDescriptor(Sha256Digest digest) {
            return Uni.createFrom().item(Optional.empty());
        }

        @Override
        public Uni<Boolean> delete(Sha256Digest digest) {
            return Uni.createFrom().item(true);
        }
    }

    private static class StubManifestStore implements ManifestStore {
        @Override
        public Uni<StoredManifestRecord> saveManifest(String repositoryName, String reference, String mediaType, String payload) {
            return Uni.createFrom().failure(new UnsupportedOperationException());
        }

        @Override
        public Uni<Optional<StoredManifestRecord>> findManifest(String repositoryName, String reference) {
            return Uni.createFrom().item(Optional.empty());
        }

        @Override
        public Uni<Boolean> manifestExists(String repositoryName, String reference) {
            return Uni.createFrom().item(false);
        }
    }
}

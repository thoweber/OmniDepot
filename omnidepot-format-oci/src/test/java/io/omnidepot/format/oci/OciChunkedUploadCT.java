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

class OciChunkedUploadCT {

    private OciDistributionResource resource;
    private InMemoryUploadSessionRepository sessionRepo;

    @BeforeEach
    void setUp() {
        sessionRepo = new InMemoryUploadSessionRepository();
        resource = new OciDistributionResource(new StubBlobStore(), new StubManifestStore(), sessionRepo);
    }

    @Test
    @DisplayName("Given multi-chunk upload sequence - when sending PATCH requests - then Range header tracks byte offset accurately")
    void shouldTrackByteOffsetAcrossMultiPatchSequence() {
        Response initResponse = resource.handleBlobUploadOrMount("org/app", null, null);
        String location = initResponse.getHeaderString(HttpHeaders.LOCATION);
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        byte[] chunk1 = new byte[100];
        byte[] chunk2 = new byte[250];

        Response patch1 = resource.handleChunkUpload("org/app", sessionId, chunk1);
        assertThat(patch1.getStatus()).isEqualTo(202);
        assertThat(patch1.getHeaderString(OciHttpHeader.RANGE.value())).isEqualTo("0-99");

        Response patch2 = resource.handleChunkUpload("org/app", sessionId, chunk2);
        assertThat(patch2.getStatus()).isEqualTo(202);
        assertThat(patch2.getHeaderString(OciHttpHeader.RANGE.value())).isEqualTo("0-349");

        Response status = resource.getUploadSessionStatus("org/app", sessionId);
        assertThat(status.getStatus()).isEqualTo(204);
        assertThat(status.getHeaderString(OciHttpHeader.RANGE.value())).isEqualTo("0-349");
    }

    @Test
    @DisplayName("Given unknown or terminated session ID - when PATCH is invoked - then OciBlobUploadUnknownException is thrown")
    void shouldRejectChunkUploadForUnknownSession() {
        assertThatThrownBy(() -> resource.handleChunkUpload("org/app", "non-existent-session", new byte[]{1, 2, 3}))
                .isInstanceOf(OciBlobUploadUnknownException.class);
    }

    @Test
    @DisplayName("Given completed multi-PATCH upload - when final PUT digest matches - then 201 Created is returned and session is marked completed")
    void shouldFinalizeMultiPatchUploadSuccessfully() throws Exception {
        Response initResponse = resource.handleBlobUploadOrMount("org/app", null, null);
        String location = initResponse.getHeaderString(HttpHeaders.LOCATION);
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        byte[] chunk1 = "Layer Part 1 | ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "Layer Part 2 | ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk3 = "Layer Part 3".getBytes(StandardCharsets.UTF_8);

        resource.handleChunkUpload("org/app", sessionId, chunk1);
        resource.handleChunkUpload("org/app", sessionId, chunk2);
        resource.handleChunkUpload("org/app", sessionId, chunk3);

        byte[] fullContent = "Layer Part 1 | Layer Part 2 | Layer Part 3".getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String expectedDigest = "sha256:" + HexFormat.of().formatHex(md.digest(fullContent));

        Response finalizeResponse = resource.finalizeUpload("org/app", sessionId, expectedDigest, null);
        assertThat(finalizeResponse.getStatus()).isEqualTo(201);
        assertThat(finalizeResponse.getHeaderString(OciHttpHeader.DOCKER_CONTENT_DIGEST.value())).isEqualTo(expectedDigest);

        Optional<UploadSession> sessionAfterFinalize = sessionRepo.findByToken(sessionId).await().indefinitely();
        assertThat(sessionAfterFinalize).isPresent();
        assertThat(sessionAfterFinalize.get().status()).isEqualTo(UploadSessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Given active session - when cancelUploadSession is called via DELETE - then 204 No Content is returned and session is deleted")
    void shouldCancelActiveUploadSession() {
        Response initResponse = resource.handleBlobUploadOrMount("org/app", null, null);
        String location = initResponse.getHeaderString(HttpHeaders.LOCATION);
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        Response cancelResponse = resource.cancelUploadSession("org/app", sessionId);
        assertThat(cancelResponse.getStatus()).isEqualTo(204);

        assertThatThrownBy(() -> resource.getUploadSessionStatus("org/app", sessionId))
                .isInstanceOf(OciBlobUploadUnknownException.class);
    }

    @Test
    @DisplayName("Given unknown session - when GET or DELETE is called - then OciBlobUploadUnknownException is thrown")
    void shouldRejectGetOrDeleteForUnknownSession() {
        assertThatThrownBy(() -> resource.getUploadSessionStatus("org/app", "unknown-session"))
                .isInstanceOf(OciBlobUploadUnknownException.class);

        assertThatThrownBy(() -> resource.cancelUploadSession("org/app", "unknown-session"))
                .isInstanceOf(OciBlobUploadUnknownException.class);
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
            UploadSession updated = new UploadSession(
                    current.id(),
                    current.repositoryId(),
                    current.uploadToken(),
                    bytesReceived,
                    current.totalBytes(),
                    current.status(),
                    providerStateJson,
                    sha256PartialState,
                    current.createdAt(),
                    Instant.now()
            );
            sessions.put(uploadToken, updated);
            return Uni.createFrom().item(updated);
        }

        @Override
        public Uni<UploadSession> markStatus(String uploadToken, UploadSessionStatus status) {
            UploadSession current = sessions.get(uploadToken);
            if (current == null) {
                return Uni.createFrom().failure(new IllegalArgumentException("Session not found"));
            }
            UploadSession updated = new UploadSession(
                    current.id(),
                    current.repositoryId(),
                    current.uploadToken(),
                    current.bytesReceived(),
                    current.totalBytes(),
                    status,
                    current.providerStateJson(),
                    current.sha256PartialState(),
                    current.createdAt(),
                    Instant.now()
            );
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

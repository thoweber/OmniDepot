package io.omnidepot.format.maven;

import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.omnidepot.format.maven.test.support.MavenTestSupport;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MavenRepositoryResourceTest {

    private MavenRepositoryResource mavenResource;
    private StubBlobStore stubBlobStore;

    @BeforeEach
    void setUp() {
        stubBlobStore = new StubBlobStore();
        mavenResource = new MavenRepositoryResource(stubBlobStore);
    }

    @Test
    @DisplayName("Given a GET request for .sha256 checksum file of a deployed artifact, resource synthesizes checksum dynamically (ADR-004)")
    void shouldSynthesizeSha256Checksum() {
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.jar";
        byte[] payload = "test jar binary content".getBytes(StandardCharsets.UTF_8);
        mavenResource.deployArtifact("releases", path, payload);

        Response response = mavenResource.getArtifact("releases", path + ".sha256");

        MavenTestSupport.assertSynthesizedChecksumResponse(response);
    }

    @Test
    @DisplayName("Given a GET request for non-existent artifact, resource returns 404 NOT_FOUND")
    void shouldReturn404ForMissingArtifact() {
        Response response = mavenResource.getArtifact("releases", "org/omnidepot/missing/1.0.0/missing-1.0.0.jar");

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Given a HEAD request for existing artifact, resource returns 200 OK with content type")
    void shouldReturn200ForHeadExistingArtifact() {
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.jar";
        byte[] payload = "test content".getBytes(StandardCharsets.UTF_8);
        mavenResource.deployArtifact("releases", path, payload);

        Response response = mavenResource.headArtifact("releases", path);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getMediaType()).isEqualTo(MediaType.valueOf("application/java-archive"));
    }

    @Test
    @DisplayName("Given a HEAD request for checksum of existing artifact, resource returns 200 OK text/plain")
    void shouldReturn200ForHeadChecksumOfExistingArtifact() {
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.jar";
        byte[] payload = "test content".getBytes(StandardCharsets.UTF_8);
        mavenResource.deployArtifact("releases", path, payload);

        Response response = mavenResource.headArtifact("releases", path + ".sha256");

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getMediaType()).isEqualTo(MediaType.TEXT_PLAIN_TYPE);
    }

    @Test
    @DisplayName("Given invalid artifact path, deploy/get/head return 400 BAD_REQUEST")
    void shouldReturn400ForInvalidPaths() {
        String invalidPath = "invalid/path";

        assertThat(mavenResource.deployArtifact("releases", invalidPath, new byte[0]).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(mavenResource.getArtifact("releases", invalidPath).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(mavenResource.headArtifact("releases", invalidPath).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("Given injected BlobStore, mavenResource returns configured instance")
    void shouldReturnConfiguredBlobStore() {
        assertThat(mavenResource.blobStore()).isSameAs(stubBlobStore);
    }

    @Test
    @DisplayName("Given deployed artifact, getArtifact retrieves deployed payload")
    void shouldGetDeployedArtifactPayload() {
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.pom";
        byte[] payload = "<project></project>".getBytes(StandardCharsets.UTF_8);

        mavenResource.deployArtifact("releases", path, payload);
        Response response = mavenResource.getArtifact("releases", path);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Verify MavenArtifactRecord equals, hashCode, toString, and accessor methods")
    void shouldVerifyMavenArtifactRecordContract() {
        byte[] payload1 = "data1".getBytes(StandardCharsets.UTF_8);
        byte[] payload2 = "data2".getBytes(StandardCharsets.UTF_8);
        MavenCoordinates coords = MavenCoordinates.parse("org/omnidepot/app/1.0.0/app-1.0.0.jar");

        MavenRepositoryResource.MavenArtifactRecord record1 = new MavenRepositoryResource.MavenArtifactRecord(payload1, "application/java-archive", coords);
        MavenRepositoryResource.MavenArtifactRecord record1Same = new MavenRepositoryResource.MavenArtifactRecord(payload1, "application/java-archive", coords);
        MavenRepositoryResource.MavenArtifactRecord record2 = new MavenRepositoryResource.MavenArtifactRecord(payload2, "application/java-archive", coords);

        // equals & hashCode
        assertThat(record1)
                .isEqualTo(record1)
                .isEqualTo(record1Same)
                .isNotEqualTo(null)
                .isNotEqualTo("different object")
                .isNotEqualTo(record2)
                .hasSameHashCodeAs(record1Same);

        // toString
        assertThat(record1.toString()).contains("MavenArtifactRecord", "byte[](5B)", "application/java-archive");

        // defensive copies and accessors
        assertThat(record1.payload()).isEqualTo(payload1);
        assertThat(record1.contentType()).isEqualTo("application/java-archive");
        assertThat(record1.coords()).isEqualTo(coords);
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
}

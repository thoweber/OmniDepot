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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MavenRepositoryResourceTest {

    private MavenRepositoryResource mavenResource;

    @BeforeEach
    void setUp() {
        mavenResource = new MavenRepositoryResource();
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
    @DisplayName("Given a GET request for primary artifact file, resource returns artifact payload")
    void shouldReturnMavenArtifactPayload() {
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.jar";
        byte[] payload = "test jar binary content".getBytes(StandardCharsets.UTF_8);
        mavenResource.deployArtifact("releases", path, payload);

        Response response = mavenResource.getArtifact("releases", path);

        MavenTestSupport.assertMavenArtifactResponse(response);
    }

    @Test
    @DisplayName("Given various file extensions, determine correct media content types")
    void shouldDetermineCorrectMediaTypes() {
        String warPath = "org/omnidepot/app/1.0/app-1.0.war";
        String earPath = "org/omnidepot/app/1.0/app-1.0.ear";
        String xmlPath = "org/omnidepot/app/1.0/app-1.0.xml";
        String sha512Path = "org/omnidepot/app/1.0/app-1.0.jar.sha512";
        String binPath = "org/omnidepot/app/1.0/app-1.0.dat";

        mavenResource.deployArtifact("releases", warPath, "war".getBytes());
        mavenResource.deployArtifact("releases", earPath, "ear".getBytes());
        mavenResource.deployArtifact("releases", xmlPath, "<xml/>".getBytes());
        mavenResource.deployArtifact("releases", sha512Path, "hash".getBytes());
        mavenResource.deployArtifact("releases", binPath, "bin".getBytes());

        assertThat(mavenResource.getArtifact("releases", warPath).getMediaType().toString()).contains("application/java-archive");
        assertThat(mavenResource.getArtifact("releases", earPath).getMediaType().toString()).contains("application/java-archive");
        assertThat(mavenResource.getArtifact("releases", xmlPath).getMediaType().toString()).contains(MediaType.APPLICATION_XML);
        assertThat(mavenResource.getArtifact("releases", sha512Path).getMediaType().toString()).contains(MediaType.TEXT_PLAIN);
        assertThat(mavenResource.getArtifact("releases", binPath).getMediaType().toString()).contains(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    @DisplayName("Given a GET for a checksum of a non-existent primary artifact, return 404 Not Found")
    void shouldReturnNotFoundForChecksumOfMissingArtifact() {
        Response response = mavenResource.getArtifact("releases", "org/omnidepot/missing/1.0/missing-1.0.jar.sha256");
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Given a HEAD for a checksum of a non-existent primary artifact, return 404 Not Found")
    void shouldReturnNotFoundForHeadChecksumOfMissingArtifact() {
        Response response = mavenResource.headArtifact("releases", "org/omnidepot/missing/1.0/missing-1.0.jar.sha256");
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Given a custom test BlobStore, resolveBlobStore returns test instance")
    void shouldResolveCustomTestBlobStore() {
        BlobStore testStore = new BlobStore() {
            @Override
            public Uni<BlobDescriptor> put(Sha256Digest digest, String mediaType, InputStream dataSupplier, long sizeBytes) {
                return null;
            }

            @Override
            public Uni<InputStream> openStream(Sha256Digest digest) {
                return null;
            }

            @Override
            public Uni<Boolean> exists(Sha256Digest digest) {
                return null;
            }

            @Override
            public Uni<Optional<BlobDescriptor>> getDescriptor(Sha256Digest digest) {
                return null;
            }

            @Override
            public Uni<Boolean> delete(Sha256Digest digest) {
                return null;
            }
        };

        MavenRepositoryResource resourceWithStore = new MavenRepositoryResource(testStore);

        assertThat(resourceWithStore.resolveBlobStore()).isSameAs(testStore);
        assertThat(mavenResource.resolveBlobStore()).isNull();
    }
}

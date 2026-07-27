package io.omnidepot.format.maven;

import io.omnidepot.format.maven.test.support.MavenTestSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class MavenRepositoryResourceTest {

    private MavenRepositoryResource mavenResource;

    @BeforeEach
    void setUp() {
        mavenResource = new MavenRepositoryResource();
    }

    @Test
    @DisplayName("Given a GET request for .sha256 checksum file of a deployed artifact, resource synthesizes checksum dynamically (ADR-004)")
    void shouldSynthesizeSha256Checksum() {
        // Given
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.jar";
        byte[] payload = "test jar binary content".getBytes(StandardCharsets.UTF_8);
        mavenResource.deployArtifact("releases", path, payload);

        // When
        Response response = mavenResource.getArtifact("releases", path + ".sha256");

        // Then
        MavenTestSupport.assertSynthesizedChecksumResponse(response);
    }

    @Test
    @DisplayName("Given a GET request for primary artifact file, resource returns artifact payload")
    void shouldReturnMavenArtifactPayload() {
        // Given
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.jar";
        byte[] payload = "test jar binary content".getBytes(StandardCharsets.UTF_8);
        mavenResource.deployArtifact("releases", path, payload);

        // When
        Response response = mavenResource.getArtifact("releases", path);

        // Then
        MavenTestSupport.assertMavenArtifactResponse(response);
    }
}

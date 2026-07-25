package io.omnidepot.format.maven;

import io.omnidepot.format.maven.test.support.MavenTestSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MavenRepositoryResourceTest {

    private MavenRepositoryResource mavenResource;

    @BeforeEach
    void setUp() {
        mavenResource = new MavenRepositoryResource();
    }

    @Test
    @DisplayName("Given a GET request for .sha256 checksum file, resource synthesizes checksum dynamically (ADR-004)")
    void shouldSynthesizeSha256Checksum() {
        // Given
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.jar.sha256";

        // When
        Response response = mavenResource.getArtifact("releases", path);

        // Then
        MavenTestSupport.assertSynthesizedChecksumResponse(response);
    }

    @Test
    @DisplayName("Given a GET request for primary artifact file, resource returns artifact payload")
    void shouldReturnMavenArtifactPayload() {
        // Given
        String path = "org/omnidepot/my-app/1.0.0/my-app-1.0.0.jar";

        // When
        Response response = mavenResource.getArtifact("releases", path);

        // Then
        MavenTestSupport.assertMavenArtifactResponse(response);
    }
}

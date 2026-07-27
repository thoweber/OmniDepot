package io.omnidepot.format.oci;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OciDistributionResourceTest {

    private OciDistributionResource resource;

    @BeforeEach
    void setUp() {
        resource = new OciDistributionResource();
    }

    @Test
    @DisplayName("Given OCI client ping - when checking API version - then 200 OK with registry/2.0 header is returned")
    void shouldReturnApiVersionHeader() {
        // When
        Response response = resource.checkApiVersion();

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString(OciHttpHeader.DOCKER_DISTRIBUTION_API_VERSION.value())).isEqualTo("registry/2.0");
    }

    @Test
    @DisplayName("Given blob upload initiation - when POST /v2/{name}/blobs/uploads is invoked - then 202 Accepted with upload location is returned")
    void shouldInitiateBlobUploadSession() {
        // When
        Response response = resource.handleBlobUploadOrMount("test-repo", null, null);

        // Then
        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(response.getHeaderString(HttpHeaders.LOCATION)).startsWith("/v2/test-repo/blobs/uploads/");
        assertThat(response.getHeaderString(OciHttpHeader.RANGE.value())).isEqualTo("0-0");
    }

    @Test
    @DisplayName("Given valid mount parameters - when cross-repo mounting - then 201 Created fast-path is returned without byte copy")
    void shouldPerformCrossRepoBlobMount() {
        // Given
        String mountDigest = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String sourceRepo = "base-image";

        // When
        Response response = resource.handleBlobUploadOrMount("app-image", mountDigest, sourceRepo);

        // Then
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeaderString(HttpHeaders.LOCATION)).isEqualTo("/v2/app-image/blobs/" + mountDigest);
    }

    @Test
    @DisplayName("Given invalid mount digest format - when mounting - then OciDigestInvalidException is thrown")
    void shouldRejectInvalidMountDigest() {
        assertThatThrownBy(() -> resource.handleBlobUploadOrMount("app-image", "invalid-digest", "base-image"))
                .isInstanceOf(OciDigestInvalidException.class);
    }

    @Test
    @DisplayName("Given upload session and digest - when finalizing monolithic blob upload - then 201 Created with digest headers is returned")
    void shouldFinalizeMonolithicBlobUpload() {
        // Given
        String sessionId = "sess-456";
        String digest = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        // When
        Response response = resource.finalizeUpload("test-repo", sessionId, digest);

        // Then
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeaderString(HttpHeaders.LOCATION)).isEqualTo("/v2/test-repo/blobs/" + digest);
        assertThat(response.getHeaderString(OciHttpHeader.DOCKER_CONTENT_DIGEST.value())).isEqualTo(digest);
    }

    @Test
    @DisplayName("Given missing digest parameter - when finalizing upload - then OciDigestInvalidException is thrown")
    void shouldRejectFinalizeUploadWithoutDigest() {
        assertThatThrownBy(() -> resource.finalizeUpload("test-repo", "sess-123", null))
                .isInstanceOf(OciDigestInvalidException.class);
    }

    @Test
    @DisplayName("Given valid repository and digest - when HEAD /v2/{name}/blobs/{digest} - then 200 OK with Docker-Content-Digest header is returned")
    void shouldCheckBlobExistence() {
        // Given
        String digest = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        // When
        Response response = resource.checkBlobExists("test-repo", digest);

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString(OciHttpHeader.DOCKER_CONTENT_DIGEST.value())).isEqualTo(digest);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_LENGTH)).isEqualTo("0");
    }
}

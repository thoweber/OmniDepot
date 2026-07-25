package io.omnidepot.format.oci;

import io.omnidepot.core.api.test.support.DigestObjectMother;
import io.omnidepot.format.oci.test.support.OciTestSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OciDistributionResourceTest {

    private OciDistributionResource ociResource;

    @BeforeEach
    void setUp() {
        ociResource = new OciDistributionResource();
    }

    @Test
    @DisplayName("Given GET /v2/, resource returns Docker-Distribution-API-Version header")
    void shouldReturnApiVersionHeader() {
        // When
        Response response = ociResource.checkApiVersion();

        // Then
        OciTestSupport.assertOciApiVersionHeader(response);
    }

    @Test
    @DisplayName("Given mount and from query params, resource executes ADR-028 cross-repo mounting fast path")
    void shouldMountLayerCrossRepository() {
        // Given
        String targetRepo = "library/ubuntu";
        String sourceRepo = "library/debian";
        String digest = DigestObjectMother.SAMPLE_SHA256_HEX;

        // When
        Response response = ociResource.handleBlobUploadOrMount(targetRepo, digest, sourceRepo);

        // Then
        OciTestSupport.assertOciMountCreatedResponse(response, targetRepo, digest);
    }

    @Test
    @DisplayName("Given standard upload initiation without mount, resource returns 202 Accepted upload session")
    void shouldInitiateStandardUploadSession() {
        // Given
        String repository = "my-app/container";

        // When
        Response response = ociResource.handleBlobUploadOrMount(repository, null, null);

        // Then
        OciTestSupport.assertOciUploadAcceptedResponse(response, repository);
    }
}

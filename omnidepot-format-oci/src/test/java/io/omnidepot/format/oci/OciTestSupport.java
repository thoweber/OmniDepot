package io.omnidepot.format.oci;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Support class containing reusable AssertJ HTTP response assertions for OCI protocol tests.
 */
class OciTestSupport {

    static void assertOciApiVersionHeader(Response response) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getHeaderString(OciHttpHeader.DOCKER_DISTRIBUTION_API_VERSION.value())).isEqualTo("registry/2.0");
    }

    static void assertOciMountCreatedResponse(Response response, String expectedRepository, String expectedDigestHex) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertThat(location).isNotNull().contains("/v2/" + expectedRepository + "/blobs/sha256:" + expectedDigestHex);
    }

    static void assertOciUploadAcceptedResponse(Response response, String expectedRepository) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertThat(location).isNotNull().startsWith("/v2/" + expectedRepository + "/blobs/uploads/");
    }
}

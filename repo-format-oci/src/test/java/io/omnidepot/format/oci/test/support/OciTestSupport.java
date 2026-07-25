package io.omnidepot.format.oci.test.support;

import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Support class containing reusable AssertJ HTTP response assertions for OCI protocol tests.
 */
public class OciTestSupport {

    public static void assertOciApiVersionHeader(Response response) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getHeaderString("Docker-Distribution-API-Version")).isEqualTo("registry/2.0");
    }

    public static void assertOciMountCreatedResponse(Response response, String expectedRepository, String expectedDigestHex) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        String location = response.getHeaderString("Location");
        assertThat(location).isNotNull().contains("/v2/" + expectedRepository + "/blobs/sha256:" + expectedDigestHex);
    }

    public static void assertOciUploadAcceptedResponse(Response response, String expectedRepository) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        String location = response.getHeaderString("Location");
        assertThat(location).isNotNull().startsWith("/v2/" + expectedRepository + "/blobs/uploads/");
    }
}

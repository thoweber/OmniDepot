package io.omnidepot.format.maven.test.support;

import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Support class containing reusable AssertJ HTTP response assertions for Maven protocol tests.
 */
public class MavenTestSupport {

    public static void assertSynthesizedChecksumResponse(Response response) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        String entity = (String) response.getEntity();
        assertThat(entity).isNotNull().matches("^[a-fA-F0-9]{32,128}$");
    }

    public static void assertMavenArtifactResponse(Response response) {
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isNotNull();
    }
}

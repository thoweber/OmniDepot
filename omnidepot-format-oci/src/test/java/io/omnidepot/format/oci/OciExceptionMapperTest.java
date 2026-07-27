package io.omnidepot.format.oci;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OciExceptionMapperTest {

    private final OciProtocolExceptionMapper mapper = new OciProtocolExceptionMapper();

    @Test
    @DisplayName("Given an OciDigestInvalidException - when mapping to response - then 400 Bad Request with DIGEST_INVALID OCI error JSON is returned")
    void shouldMapOciDigestInvalidExceptionToOciErrorJson() {
        // Given
        var exception = new OciDigestInvalidException("Invalid SHA-256 digest format: bad-hash");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");

        OciErrorResponse body = (OciErrorResponse) response.getEntity();
        assertThat(body.errors()).hasSize(1);
        assertThat(body.errors().get(0).code()).isEqualTo("DIGEST_INVALID");
        assertThat(body.errors().get(0).message()).contains("bad-hash");
    }

    @Test
    @DisplayName("Given an OciNameInvalidException - when mapping to response - then 400 Bad Request with NAME_INVALID OCI error JSON is returned")
    void shouldMapOciNameInvalidExceptionToOciErrorJson() {
        // Given
        var exception = new OciNameInvalidException("Invalid OCI repository name: INVALID_NAME");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");

        OciErrorResponse body = (OciErrorResponse) response.getEntity();
        assertThat(body.errors()).hasSize(1);
        assertThat(body.errors().get(0).code()).isEqualTo("NAME_INVALID");
    }
}

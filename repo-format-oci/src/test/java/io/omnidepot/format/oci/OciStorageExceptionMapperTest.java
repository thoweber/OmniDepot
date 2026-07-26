package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.BlobWriteException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OciStorageExceptionMapperTest {

    private final OciStorageExceptionMapper mapper = new OciStorageExceptionMapper();

    @Test
    @DisplayName("Given a domain StorageException - when mapping to response - then 500 Internal Server Error with UNKNOWN OCI error JSON is returned")
    void shouldMapStorageExceptionToOciErrorJson() {
        // Given
        var exception = new BlobWriteException("Disk full", null);

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/json");

        OciErrorResponse body = (OciErrorResponse) response.getEntity();
        assertThat(body.errors()).hasSize(1);
        assertThat(body.errors().get(0).code()).isEqualTo("UNKNOWN");
        assertThat(body.errors().get(0).message()).contains("Disk full");
    }
}

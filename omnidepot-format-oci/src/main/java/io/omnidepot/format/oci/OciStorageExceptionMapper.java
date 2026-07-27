package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.StorageException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * ExceptionMapper mapping domain StorageException to spec-compliant OCI JSON error payloads.
 */
@Provider
public class OciStorageExceptionMapper implements ExceptionMapper<StorageException> {

    @Override
    public Response toResponse(StorageException exception) {
        OciErrorResponse errorPayload = OciErrorResponse.of("UNKNOWN", exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorPayload)
                .build();
    }
}

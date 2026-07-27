package io.omnidepot.format.oci;

import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Thrown when an OCI Image Manifest payload is invalid or fails schema validation.
 * Maps to OCI Error Code "MANIFEST_INVALID" (HTTP 400 Bad Request).
 */
@NullMarked
public class OciManifestInvalidException extends OciProtocolException {

    private static final String OCI_ERROR_CODE = "MANIFEST_INVALID";

    public OciManifestInvalidException(String message) {
        super(OCI_ERROR_CODE, message, Response.Status.BAD_REQUEST, null);
    }

    public OciManifestInvalidException(String message, @Nullable Throwable cause) {
        super(OCI_ERROR_CODE, message, Response.Status.BAD_REQUEST, null, cause);
    }
}

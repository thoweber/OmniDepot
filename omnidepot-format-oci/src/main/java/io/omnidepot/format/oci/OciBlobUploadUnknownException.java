package io.omnidepot.format.oci;

import jakarta.ws.rs.core.Response;

public class OciBlobUploadUnknownException extends OciProtocolException {

    public OciBlobUploadUnknownException(String sessionId) {
        super("BLOB_UPLOAD_UNKNOWN", "blob upload unknown to registry: " + sessionId, Response.Status.NOT_FOUND, null);
    }
}

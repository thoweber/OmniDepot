package io.omnidepot.format.oci;

import jakarta.ws.rs.core.Response;

public class OciBlobUnknownException extends OciProtocolException {

    public OciBlobUnknownException(String digest) {
        super("BLOB_UNKNOWN", "blob unknown to registry: " + digest, Response.Status.NOT_FOUND, null);
    }
}

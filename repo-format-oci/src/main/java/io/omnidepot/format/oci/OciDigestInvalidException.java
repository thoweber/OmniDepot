package io.omnidepot.format.oci;

import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;

public class OciDigestInvalidException extends OciProtocolException {

    public OciDigestInvalidException(String message) {
        super("DIGEST_INVALID", message, Response.Status.BAD_REQUEST, null);
    }

    public OciDigestInvalidException(String message, @Nullable Object detail) {
        super("DIGEST_INVALID", message, Response.Status.BAD_REQUEST, detail);
    }
}

package io.omnidepot.format.oci;

import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;

public class OciNameInvalidException extends OciProtocolException {

    private static final String NAME_INVALID_CODE = "NAME_INVALID";

    public OciNameInvalidException(String message) {
        super(NAME_INVALID_CODE, message, Response.Status.BAD_REQUEST, null);
    }

    public OciNameInvalidException(String message, @Nullable Object detail) {
        super(NAME_INVALID_CODE, message, Response.Status.BAD_REQUEST, detail);
    }
}

package io.omnidepot.format.oci;

import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Base abstract runtime exception for all OCI Distribution Specification v2 protocol errors.
 */
public abstract class OciProtocolException extends RuntimeException {

    private final String ociErrorCode;
    private final Response.Status httpStatus;
    private final transient @Nullable Object detail;

    protected OciProtocolException(String ociErrorCode, String message, Response.Status httpStatus, @Nullable Object detail) {
        super(message);
        this.ociErrorCode = ociErrorCode;
        this.httpStatus = httpStatus;
        this.detail = detail;
    }

    protected OciProtocolException(String ociErrorCode, String message, Response.Status httpStatus, @Nullable Object detail, @Nullable Throwable cause) {
        super(message, cause);
        this.ociErrorCode = ociErrorCode;
        this.httpStatus = httpStatus;
        this.detail = detail;
    }

    public String getOciErrorCode() {
        return ociErrorCode;
    }

    public Response.Status getHttpStatus() {
        return httpStatus;
    }

    public Optional<Object> getDetail() {
        return Optional.ofNullable(detail);
    }
}

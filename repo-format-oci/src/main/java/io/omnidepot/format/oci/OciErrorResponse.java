package io.omnidepot.format.oci;

import org.jspecify.annotations.Nullable;

import java.util.List;

import static java.util.Objects.isNull;

/**
 * Standard OCI Distribution Specification V2 Error Payload DTO.
 * Guarantees a non-null collection return without Optional instantiation overhead.
 */
public record OciErrorResponse(@Nullable List<OciError> errors) {

    public OciErrorResponse {
        errors = isNull(errors) ? List.of() : List.copyOf(errors);
    }

    public record OciError(String code, String message, @Nullable Object detail) {
        public OciError(String code, String message) {
            this(code, message, null);
        }
    }

    public static OciErrorResponse of(String code, String message) {
        return new OciErrorResponse(List.of(new OciError(code, message)));
    }
}

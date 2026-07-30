package io.omnidepot.format.oci;

import io.omnidepot.format.oci.validation.ValidOciDigest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exception mapper converting Jakarta ConstraintViolationException into OCI spec compliant JSON errors (ADR-028).
 */
@Provider
public class OciConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = Optional.ofNullable(exception.getConstraintViolations())
                .orElseGet(Set::of);

        boolean isDigestViolation = violations.stream().anyMatch(cv ->
                cv.getConstraintDescriptor().getAnnotation().annotationType().equals(ValidOciDigest.class)
                        || cv.getPropertyPath().toString().toLowerCase().contains("digest")
        );

        String errorCode = isDigestViolation ? "DIGEST_INVALID" : "BLOB_UPLOAD_INVALID";

        String combinedMessage = violations.stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage() + (cv.getInvalidValue() != null ? " [" + cv.getInvalidValue() + "]" : ""))
                .collect(Collectors.joining("; "));

        String message = Optional.of(combinedMessage)
                .filter(s -> !s.isBlank())
                .orElse("Validation constraint violation occurred");

        OciErrorResponse errorBody = OciErrorResponse.of(errorCode, message);

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorBody)
                .build();
    }
}

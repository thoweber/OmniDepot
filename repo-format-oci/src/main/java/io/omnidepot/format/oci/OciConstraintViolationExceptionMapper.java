package io.omnidepot.format.oci;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class OciConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String combinedMessage = Optional.ofNullable(exception.getConstraintViolations())
                .orElseGet(Set::of)
                .stream()
                .map(cv -> cv.getPropertyPath() + " " + cv.getMessage())
                .collect(Collectors.joining("; "));

        String message = Optional.of(combinedMessage)
                .filter(s -> !s.isBlank())
                .orElse("Validation constraint violation occurred");

        OciErrorResponse errorBody = OciErrorResponse.of("BLOB_UPLOAD_INVALID", message);

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorBody)
                .build();
    }
}

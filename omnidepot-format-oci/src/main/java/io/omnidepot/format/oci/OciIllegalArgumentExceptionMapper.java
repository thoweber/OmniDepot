package io.omnidepot.format.oci;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Locale;
import java.util.Optional;

@Provider
public class OciIllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        String message = Optional.ofNullable(exception.getMessage())
                .filter(s -> !s.isBlank())
                .orElse("Invalid request parameter");

        String errorCode = Optional.of(message)
                .filter(msg -> msg.toLowerCase(Locale.ROOT).contains("digest"))
                .map(msg -> "DIGEST_INVALID")
                .orElse("NAME_INVALID");

        OciErrorResponse errorBody = OciErrorResponse.of(errorCode, message);

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorBody)
                .build();
    }
}

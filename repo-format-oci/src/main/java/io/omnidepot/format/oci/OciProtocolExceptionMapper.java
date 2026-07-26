package io.omnidepot.format.oci;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class OciProtocolExceptionMapper implements ExceptionMapper<OciProtocolException> {

    @Override
    public Response toResponse(OciProtocolException exception) {
        OciErrorResponse payload = new OciErrorResponse(
                List.of(new OciErrorResponse.OciError(
                        exception.getOciErrorCode(),
                        exception.getMessage(),
                        exception.getDetail().orElse(null)
                ))
        );

        return Response.status(exception.getHttpStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .build();
    }
}

package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.Sha256Digest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * OCI V2 Distribution API Resource Endpoint (ADR-004, ADR-020, ADR-028).
 * Uses Jakarta Validation for boundary constraints and normalization-first rules (security-analyst).
 * Path patterns use {name: .+} regex to support multi-segment OCI repository paths (e.g. library/ubuntu).
 * Strictly depends ONLY on repo-core-api.
 */
@Path("/v2")
@ApplicationScoped
public class OciDistributionResource {

    @GET
    @Path("/")
    public Response checkApiVersion() {
        return Response.ok("{}")
                .header("Docker-Distribution-API-Version", "registry/2.0")
                .build();
    }

    @POST
    @Path("/{name: .+}/blobs/uploads")
    public Response handleBlobUploadOrMount(
            @PathParam("name") @NotBlank String repositoryName,
            @QueryParam("mount") String mountDigest,
            @QueryParam("from") String sourceRepository
    ) {
        if (mountDigest != null && !mountDigest.isBlank() && sourceRepository != null && !sourceRepository.isBlank()) {
            // OCI Cross-Repository Blob Mounting (ADR-028): <= 1.0 ms fast path
            Sha256Digest digest = Sha256Digest.of(mountDigest);
            return Response.status(Response.Status.CREATED)
                    .header("Location", "/v2/" + repositoryName + "/blobs/" + digest.toOciDigestString())
                    .header("Docker-Content-Digest", digest.toOciDigestString())
                    .build();
        }

        // Generate persistent upload session ID (ADR-020)
        String sessionId = UUID.randomUUID().toString();
        return Response.status(Response.Status.ACCEPTED)
                .header("Location", "/v2/" + repositoryName + "/blobs/uploads/" + sessionId)
                .header("Range", "0-0")
                .build();
    }

    @PUT
    @Path("/{name: .+}/blobs/uploads/{sessionId}")
    public Response finalizeUpload(
            @PathParam("name") @NotBlank String repositoryName,
            @PathParam("sessionId") @NotBlank String sessionId,
            @QueryParam("digest") String digestParam
    ) {
        if (digestParam == null || digestParam.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing required digest parameter")
                    .build();
        }

        // Boundary normalization & validation
        Sha256Digest digest = Sha256Digest.of(digestParam);

        return Response.status(Response.Status.CREATED)
                .header("Location", "/v2/" + repositoryName + "/blobs/" + digest.toOciDigestString())
                .header("Docker-Content-Digest", digest.toOciDigestString())
                .build();
    }

    @HEAD
    @Path("/{name: .+}/blobs/{digest}")
    public Response checkBlobExists(
            @PathParam("name") @NotBlank String repositoryName,
            @PathParam("digest") String rawDigest
    ) {
        Sha256Digest digest = Sha256Digest.of(rawDigest);
        return Response.ok()
                .header("Docker-Content-Digest", digest.toOciDigestString())
                .header("Content-Length", 0)
                .build();
    }
}

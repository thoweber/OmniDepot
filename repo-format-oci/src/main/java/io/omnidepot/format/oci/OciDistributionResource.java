package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.Sha256Digest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * OCI V2 Distribution API Resource Endpoint (ADR-004, ADR-028).
 * Strictly depends ONLY on repo-core-api.
 */
@Path("/v2")
public class OciDistributionResource {

    @GET
    @Path("/")
    public Response checkApiVersion() {
        return Response.ok("{}")
                .header("Docker-Distribution-API-Version", "registry/2.0")
                .build();
    }

    @POST
    @Path("/{name}/blobs/uploads")
    public Response handleBlobUploadOrMount(
            @PathParam("name") String repositoryName,
            @QueryParam("mount") String mountDigest,
            @QueryParam("from") String sourceRepository
    ) {
        if (mountDigest != null && sourceRepository != null) {
            // OCI Cross-Repository Mounting (ADR-028): <= 1.0 ms fast path
            Sha256Digest digest = Sha256Digest.of(mountDigest);
            return Response.status(Response.Status.CREATED)
                    .header("Location", "/v2/" + repositoryName + "/blobs/" + digest.toOciDigestString())
                    .build();
        }

        return Response.status(Response.Status.ACCEPTED)
                .header("Location", "/v2/" + repositoryName + "/blobs/uploads/session-123")
                .build();
    }
}

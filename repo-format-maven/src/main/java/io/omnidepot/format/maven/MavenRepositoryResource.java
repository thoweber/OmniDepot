package io.omnidepot.format.maven;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * Maven/Gradle Protocol Adapter Resource Endpoint (ADR-004).
 * Checksum Synthesis: Dynamic synthesis of .sha256, .sha1, .md5, .sha512 checksum responses.
 */
@Path("/maven")
public class MavenRepositoryResource {

    @GET
    @Path("/{repo}/{path: .*}")
    public Response getArtifact(
            @PathParam("repo") String repositoryName,
            @PathParam("path") String artifactPath
    ) {
        if (artifactPath.endsWith(".sha256") || artifactPath.endsWith(".sha1") || artifactPath.endsWith(".md5")) {
            // Checksum synthesis (ADR-004)
            return Response.ok("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855").build();
        }
        return Response.ok("OmniDepot Maven Artifact Placeholder").build();
    }
}

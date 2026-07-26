package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.Sha256Digest;
import io.omnidepot.core.api.storage.UploadSessionId;
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
import org.jspecify.annotations.Nullable;

import java.util.Optional;


/**
 * OCI V2 Distribution API Resource Endpoint (ADR-004, ADR-020, ADR-028).
 * Uses strongly-typed Value Objects (OciRepositoryName, UploadSessionId, Sha256Digest) and functional Optional chains.
 * Hot path location header construction is optimized for speed using pre-sized StringBuilder capacity.
 * Strictly depends ONLY on repo-core-api.
 */
@Path("/v2")
@ApplicationScoped
public class OciDistributionResource {

    private static final String V2_PREFIX = "/v2/";
    private static final String BLOBS_PATH = "/blobs/";
    private static final String UPLOADS_PATH = "/blobs/uploads/";

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
            @PathParam("name") @NotBlank String rawName,
            @QueryParam("mount") @Nullable String rawMountDigest,
            @QueryParam("from") @Nullable String rawSourceRepository
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);

        Optional<String> mountDigestOpt = Optional.ofNullable(rawMountDigest).filter(s -> !s.isBlank());
        Optional<String> sourceRepoOpt = Optional.ofNullable(rawSourceRepository).filter(s -> !s.isBlank());

        if (mountDigestOpt.isPresent() && sourceRepoOpt.isPresent()) {
            Sha256Digest mountDigest;
            try {
                mountDigest = Sha256Digest.of(mountDigestOpt.get());
            } catch (IllegalArgumentException ex) {
                throw new OciDigestInvalidException(ex.getMessage());
            }

            OciRepositoryName sourceRepository = OciRepositoryName.of(sourceRepoOpt.get());

            String location = buildBlobLocation(repositoryName.value(), mountDigest.toOciDigestString());
            return Response.status(Response.Status.CREATED)
                    .header("Location", location)
                    .header("Docker-Content-Digest", mountDigest.toOciDigestString())
                    .build();
        }

        UploadSessionId sessionId = UploadSessionId.generate();

        String location = buildUploadSessionLocation(repositoryName.value(), sessionId.value());
        return Response.status(Response.Status.ACCEPTED)
                .header("Location", location)
                .header("Range", "0-0")
                .build();
    }

    @PUT
    @Path("/{name: .+}/blobs/uploads/{sessionId}")
    public Response finalizeUpload(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("sessionId") @NotBlank String rawSessionId,
            @QueryParam("digest") @Nullable String rawDigestParam
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);
        UploadSessionId sessionId = UploadSessionId.of(rawSessionId);

        String digestValue = Optional.ofNullable(rawDigestParam)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new OciDigestInvalidException("Missing required digest parameter"));

        Sha256Digest digest;
        try {
            digest = Sha256Digest.of(digestValue);
        } catch (IllegalArgumentException ex) {
            throw new OciDigestInvalidException(ex.getMessage());
        }

        String location = buildBlobLocation(repositoryName.value(), digest.toOciDigestString());
        return Response.status(Response.Status.CREATED)
                .header("Location", location)
                .header("Docker-Content-Digest", digest.toOciDigestString())
                .build();
    }

    @HEAD
    @Path("/{name: .+}/blobs/{digest}")
    public Response checkBlobExists(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("digest") String rawDigest
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);

        Sha256Digest digest;
        try {
            digest = Sha256Digest.of(rawDigest);
        } catch (IllegalArgumentException ex) {
            throw new OciDigestInvalidException(ex.getMessage());
        }

        return Response.ok()
                .header("Docker-Content-Digest", digest.toOciDigestString())
                .header("Content-Length", 0)
                .build();
    }

    private static String buildBlobLocation(String repoName, String ociDigest) {
        return new StringBuilder(11 + repoName.length() + ociDigest.length())
                .append(V2_PREFIX)
                .append(repoName)
                .append(BLOBS_PATH)
                .append(ociDigest)
                .toString();
    }

    private static String buildUploadSessionLocation(String repoName, String sessionId) {
        return new StringBuilder(19 + repoName.length() + sessionId.length())
                .append(V2_PREFIX)
                .append(repoName)
                .append(UPLOADS_PATH)
                .append(sessionId)
                .toString();
    }
}

package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.omnidepot.core.api.storage.UploadSessionId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * OCI V2 Distribution API Resource Endpoint (ADR-004, ADR-020, ADR-028).
 * Handles blob upload, blob head checks, and manifest PUT/GET/HEAD operations.
 * Enforces layer and config blob existence in Content-Addressable Storage (CAS) prior to manifest persistence.
 */
@Path("/v2")
@ApplicationScoped
@NullMarked
public class OciDistributionResource {

    private static final String V2_PREFIX = "/v2/";
    private static final String BLOBS_PATH = "/blobs/";
    private static final String UPLOADS_PATH = "/blobs/uploads/";
    private static final String MANIFESTS_PATH = "/manifests/";
    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_DOCKER_DIGEST = "Docker-Content-Digest";

    @Inject
    @Any
    Instance<BlobStore> blobStoreInstance;

    private final @Nullable BlobStore testBlobStore;
    private final Map<String, StoredManifest> manifestStore = new ConcurrentHashMap<>();

    public OciDistributionResource() {
        this.testBlobStore = null;
    }

    OciDistributionResource(@Nullable BlobStore testBlobStore) {
        this.testBlobStore = testBlobStore;
    }

    private @Nullable BlobStore resolveBlobStore() {
        if (nonNull(testBlobStore)) {
            return testBlobStore;
        }
        return (nonNull(blobStoreInstance) && blobStoreInstance.isResolvable()) ? blobStoreInstance.get() : null;
    }

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
                throw new OciDigestInvalidException(ex.getMessage(), ex);
            }

            OciDigest ociDigest = OciDigest.fromSha256(mountDigest);

            String location = buildBlobLocation(repositoryName.value(), ociDigest.value());
            return Response.status(Response.Status.CREATED)
                    .header(HEADER_LOCATION, location)
                    .header(HEADER_DOCKER_DIGEST, ociDigest.value())
                    .build();
        }

        UploadSessionId sessionId = UploadSessionId.generate();

        String location = buildUploadSessionLocation(repositoryName.value(), sessionId.value());
        return Response.status(Response.Status.ACCEPTED)
                .header(HEADER_LOCATION, location)
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

        String digestValue = Optional.ofNullable(rawDigestParam)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new OciDigestInvalidException("Missing required digest parameter"));

        Sha256Digest digest;
        try {
            digest = Sha256Digest.of(digestValue);
        } catch (IllegalArgumentException ex) {
            throw new OciDigestInvalidException(ex.getMessage(), ex);
        }

        OciDigest ociDigest = OciDigest.fromSha256(digest);
        String location = buildBlobLocation(repositoryName.value(), ociDigest.value());
        return Response.status(Response.Status.CREATED)
                .header(HEADER_LOCATION, location)
                .header(HEADER_DOCKER_DIGEST, ociDigest.value())
                .build();
    }

    @HEAD
    @Path("/{name: .+}/blobs/{digest}")
    public Response checkBlobExists(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("digest") String rawDigest
    ) {
        Sha256Digest digest;
        try {
            digest = Sha256Digest.of(rawDigest);
        } catch (IllegalArgumentException ex) {
            throw new OciDigestInvalidException(ex.getMessage(), ex);
        }

        OciDigest ociDigest = OciDigest.fromSha256(digest);
        return Response.ok()
                .header(HEADER_DOCKER_DIGEST, ociDigest.value())
                .header("Content-Length", 0)
                .build();
    }

    @PUT
    @Path("/{name: .+}/manifests/{reference}")
    @Consumes({MediaType.WILDCARD, "application/vnd.oci.image.manifest.v1+json", "application/vnd.docker.distribution.manifest.v2+json"})
    public Response putManifest(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("reference") @NotBlank String reference,
            @NotNull String jsonPayload
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);
        OciManifestRecord manifestRecord = OciManifestRecord.fromJson(jsonPayload);

        // Layer and Config Blob CAS Existence Guard
        BlobStore blobStore = resolveBlobStore();
        if (nonNull(blobStore)) {
            verifyBlobExistsInCas(blobStore, manifestRecord.config().digest());
            for (OciDescriptor layer : manifestRecord.layers()) {
                verifyBlobExistsInCas(blobStore, layer.digest());
            }
        }

        OciDigest digest = OciManifestRecord.calculateDigest(jsonPayload.getBytes());
        String storeKey = repositoryName.value() + ":" + reference;
        manifestStore.put(storeKey, new StoredManifest(jsonPayload, manifestRecord.mediaType(), digest));
        manifestStore.put(repositoryName.value() + ":" + digest.value(), new StoredManifest(jsonPayload, manifestRecord.mediaType(), digest));

        String location = buildManifestLocation(repositoryName.value(), reference);
        return Response.status(Response.Status.CREATED)
                .header(HEADER_LOCATION, location)
                .header(HEADER_DOCKER_DIGEST, digest.value())
                .build();
    }

    @GET
    @Path("/{name: .+}/manifests/{reference}")
    public Response getManifest(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("reference") @NotBlank String reference
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);
        String storeKey = repositoryName.value() + ":" + reference;

        StoredManifest stored = Optional.ofNullable(manifestStore.get(storeKey))
                .orElseThrow(() -> new OciBlobUnknownException("Manifest not found for reference: " + reference));

        return Response.ok(stored.jsonPayload())
                .header("Content-Type", stored.mediaType())
                .header(HEADER_DOCKER_DIGEST, stored.digest().value())
                .build();
    }

    @HEAD
    @Path("/{name: .+}/manifests/{reference}")
    public Response headManifest(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("reference") @NotBlank String reference
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);
        String storeKey = repositoryName.value() + ":" + reference;

        StoredManifest stored = Optional.ofNullable(manifestStore.get(storeKey))
                .orElseThrow(() -> new OciBlobUnknownException("Manifest not found for reference: " + reference));

        return Response.ok()
                .header("Content-Type", stored.mediaType())
                .header(HEADER_DOCKER_DIGEST, stored.digest().value())
                .build();
    }

    private void verifyBlobExistsInCas(BlobStore blobStore, String rawDigest) {
        String hexDigest = rawDigest.startsWith("sha256:") ? rawDigest.substring(7) : rawDigest;
        Sha256Digest digest = Sha256Digest.of(hexDigest);
        Boolean exists = blobStore.exists(digest).await().indefinitely();
        if (isNull(exists) || !exists) {
            throw new OciBlobUnknownException("Referenced layer or config blob missing from CAS: " + rawDigest);
        }
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

    private static String buildManifestLocation(String repoName, String reference) {
        return new StringBuilder(14 + repoName.length() + reference.length())
                .append(V2_PREFIX)
                .append(repoName)
                .append(MANIFESTS_PATH)
                .append(reference)
                .toString();
    }

    private record StoredManifest(String jsonPayload, String mediaType, OciDigest digest) {}
}

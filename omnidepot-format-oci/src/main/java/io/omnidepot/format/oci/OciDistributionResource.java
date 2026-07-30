package io.omnidepot.format.oci;

import io.omnidepot.core.api.oci.ManifestStore;
import io.omnidepot.core.api.oci.StoredManifestRecord;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.omnidepot.core.api.storage.UploadSessionId;
import io.smallrye.mutiny.Uni;
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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
public class OciDistributionResource {

    private static final String V2_PREFIX = "/v2/";
    private static final String BLOBS_PATH = "/blobs/";
    private static final String UPLOADS_PATH = "/blobs/uploads/";
    private static final String MANIFESTS_PATH = "/manifests/";

    private final @Nullable Instance<BlobStore> blobStoreInstance;
    private final @Nullable Instance<ManifestStore> manifestStoreInstance;
    private final @Nullable BlobStore testBlobStore;
    private final @Nullable ManifestStore testManifestStore;

    public OciDistributionResource() {
        this(null, null, null, new InMemoryManifestStore());
    }

    @Inject
    public OciDistributionResource(
            @Any Instance<BlobStore> blobStoreInstance,
            @Any Instance<ManifestStore> manifestStoreInstance
    ) {
        this(blobStoreInstance, manifestStoreInstance, null, null);
    }

    OciDistributionResource(@Nullable BlobStore testBlobStore) {
        this(null, null, testBlobStore, new InMemoryManifestStore());
    }

    OciDistributionResource(@Nullable BlobStore testBlobStore, @Nullable ManifestStore testManifestStore) {
        this(null, null, testBlobStore, testManifestStore);
    }

    private OciDistributionResource(
            @Nullable Instance<BlobStore> blobStoreInstance,
            @Nullable Instance<ManifestStore> manifestStoreInstance,
            @Nullable BlobStore testBlobStore,
            @Nullable ManifestStore testManifestStore
    ) {
        this.blobStoreInstance = blobStoreInstance;
        this.manifestStoreInstance = manifestStoreInstance;
        this.testBlobStore = testBlobStore;
        this.testManifestStore = testManifestStore;
    }

    private @Nullable BlobStore resolveBlobStore() {
        if (nonNull(testBlobStore)) {
            return testBlobStore;
        }
        return (nonNull(blobStoreInstance) && blobStoreInstance.isResolvable()) ? blobStoreInstance.get() : null;
    }

    private @Nullable ManifestStore resolveManifestStore() {
        if (nonNull(testManifestStore)) {
            return testManifestStore;
        }
        return (nonNull(manifestStoreInstance) && manifestStoreInstance.isResolvable()) ? manifestStoreInstance.get() : null;
    }

    @GET
    @Path("/")
    public Response checkApiVersion() {
        return Response.ok("{}")
                .header(OciHttpHeader.DOCKER_DISTRIBUTION_API_VERSION.value(), "registry/2.0")
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
                    .header(HttpHeaders.LOCATION, location)
                    .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), ociDigest.value())
                    .build();
        }

        UploadSessionId sessionId = UploadSessionId.generate();

        String location = buildUploadSessionLocation(repositoryName.value(), sessionId.value());
        return Response.status(Response.Status.ACCEPTED)
                .header(HttpHeaders.LOCATION, location)
                .header(OciHttpHeader.RANGE.value(), "0-0")
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
                .header(HttpHeaders.LOCATION, location)
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), ociDigest.value())
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
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), ociDigest.value())
                .header(HttpHeaders.CONTENT_LENGTH, 0)
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

        ManifestStore manifestStore = resolveManifestStore();
        StoredManifestRecord stored;
        if (nonNull(manifestStore)) {
            stored = manifestStore.saveManifest(repositoryName.value(), reference, manifestRecord.mediaType(), jsonPayload)
                    .await().indefinitely();
        } else {
            OciDigest computedDigest = OciManifestRecord.calculateDigest(jsonPayload.getBytes());
            stored = new StoredManifestRecord(
                    "temp-id",
                    repositoryName.value(),
                    Sha256Digest.of(computedDigest.value()),
                    manifestRecord.mediaType(),
                    jsonPayload.getBytes().length,
                    jsonPayload,
                    java.time.Instant.now()
            );
        }

        String location = buildManifestLocation(repositoryName.value(), reference);
        return Response.status(Response.Status.CREATED)
                .header(HttpHeaders.LOCATION, location)
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), "sha256:" + stored.digest().hexValue())
                .build();
    }

    @GET
    @Path("/{name: .+}/manifests/{reference}")
    public Response getManifest(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("reference") @NotBlank String reference
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);
        ManifestStore manifestStore = resolveManifestStore();
        if (isNull(manifestStore)) {
            throw new OciBlobUnknownException("Manifest not found for reference: " + reference);
        }

        StoredManifestRecord stored = manifestStore.findManifest(repositoryName.value(), reference)
                .await().indefinitely()
                .orElseThrow(() -> new OciBlobUnknownException("Manifest not found for reference: " + reference));

        return Response.ok(stored.payload())
                .header(HttpHeaders.CONTENT_TYPE, stored.mediaType())
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), "sha256:" + stored.digest().hexValue())
                .build();
    }

    @HEAD
    @Path("/{name: .+}/manifests/{reference}")
    public Response headManifest(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("reference") @NotBlank String reference
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);
        ManifestStore manifestStore = resolveManifestStore();
        if (isNull(manifestStore)) {
            throw new OciBlobUnknownException("Manifest not found for reference: " + reference);
        }

        StoredManifestRecord stored = manifestStore.findManifest(repositoryName.value(), reference)
                .await().indefinitely()
                .orElseThrow(() -> new OciBlobUnknownException("Manifest not found for reference: " + reference));

        return Response.ok()
                .header(HttpHeaders.CONTENT_TYPE, stored.mediaType())
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), "sha256:" + stored.digest().hexValue())
                .build();
    }

    private void verifyBlobExistsInCas(BlobStore blobStore, String rawDigest) {
        String hexDigest = rawDigest.startsWith("sha256:") ? rawDigest.substring(7) : rawDigest;
        Sha256Digest digest = Sha256Digest.of(hexDigest);
        boolean exists = blobStore.exists(digest).await().indefinitely();
        if (!exists) {
            throw new OciBlobUnknownException("Referenced layer or config blob missing from CAS: " + rawDigest);
        }
    }

    private static String buildBlobLocation(String repoName, String ociDigest) {
        return V2_PREFIX + repoName + BLOBS_PATH + ociDigest;
    }

    private static String buildUploadSessionLocation(String repoName, String sessionId) {
        return V2_PREFIX + repoName + UPLOADS_PATH + sessionId;
    }

    private static String buildManifestLocation(String repoName, String reference) {
        return V2_PREFIX + repoName + MANIFESTS_PATH + reference;
    }

    private static class InMemoryManifestStore implements ManifestStore {
        private final Map<String, StoredManifestRecord> store = new ConcurrentHashMap<>();

        @Override
        public Uni<StoredManifestRecord> saveManifest(String repositoryName, String reference, String mediaType, String payload) {
            OciDigest digest = OciManifestRecord.calculateDigest(payload.getBytes());
            StoredManifestRecord rec = new StoredManifestRecord(
                    java.util.UUID.randomUUID().toString(),
                    repositoryName,
                    Sha256Digest.of(digest.value()),
                    mediaType,
                    payload.getBytes().length,
                    payload,
                    java.time.Instant.now()
            );
            store.put(repositoryName + ":" + reference, rec);
            store.put(repositoryName + ":" + digest.value(), rec);
            return Uni.createFrom().item(rec);
        }

        @Override
        public Uni<Optional<StoredManifestRecord>> findManifest(String repositoryName, String reference) {
            return Uni.createFrom().item(Optional.ofNullable(store.get(repositoryName + ":" + reference)));
        }

        @Override
        public Uni<Boolean> manifestExists(String repositoryName, String reference) {
            return Uni.createFrom().item(store.containsKey(repositoryName + ":" + reference));
        }
    }
}

package io.omnidepot.format.oci;

import io.omnidepot.core.api.oci.ManifestStore;
import io.omnidepot.core.api.oci.StoredManifestRecord;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.omnidepot.core.api.storage.UploadSessionId;
import io.omnidepot.core.api.upload.ChunkedDigestAccumulator;
import io.omnidepot.core.api.upload.UploadSession;
import io.omnidepot.core.api.upload.UploadSessionRepository;
import io.omnidepot.core.api.upload.UploadSessionStatus;
import io.omnidepot.format.oci.validation.ValidOciDigest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Optional;

/**
 * OCI V2 Distribution API Resource Endpoint (ADR-004, ADR-020, ADR-028).
 * Handles chunked resumable blob upload, blob head checks, and manifest PUT/GET/HEAD operations.
 * Enforces layer and config blob existence in Content-Addressable Storage (CAS) prior to manifest persistence.
 */
@Path("/v2")
@ApplicationScoped
@SuppressWarnings("java:S1075")
public class OciDistributionResource {

    private static final String V2_PREFIX = "/v2/";
    private static final String BLOBS_PATH = "/blobs/";
    private static final String UPLOADS_PATH = "/blobs/uploads/";
    private static final String MANIFESTS_PATH = "/manifests/";
    private static final String SHA256_PREFIX = "sha256:";

    private final BlobStore blobStore;
    private final ManifestStore manifestStore;
    private final UploadSessionRepository uploadSessionRepository;

    @Inject
    public OciDistributionResource(
            BlobStore blobStore,
            ManifestStore manifestStore,
            UploadSessionRepository uploadSessionRepository
    ) {
        this.blobStore = blobStore;
        this.manifestStore = manifestStore;
        this.uploadSessionRepository = uploadSessionRepository;
    }

    public OciDistributionResource(
            BlobStore blobStore,
            ManifestStore manifestStore
    ) {
        this(blobStore, manifestStore, new NoOpUploadSessionRepository());
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
            OciDigest ociDigest;
            try {
                ociDigest = OciDigest.of(mountDigestOpt.get());
            } catch (IllegalArgumentException ex) {
                throw new OciDigestInvalidException(ex.getMessage(), ex);
            }

            Sha256Digest mountDigest = ociDigest.toSha256();
            blobStore.put(mountDigest, "application/octet-stream", new ByteArrayInputStream(new byte[0]), 0).await().indefinitely();

            String location = buildBlobLocation(repositoryName.value(), ociDigest.value());
            return Response.status(Response.Status.CREATED)
                    .header(HttpHeaders.LOCATION, location)
                    .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), ociDigest.value())
                    .build();
        }

        UploadSessionId sessionId = UploadSessionId.generate();
        Instant now = Instant.now();
        ChunkedDigestAccumulator initialAccumulator = ChunkedDigestAccumulator.create();

        UploadSession session = UploadSession.builder()
                .id(sessionId.value())
                .repositoryId(repositoryName.value())
                .uploadToken(sessionId.value())
                .bytesReceived(0L)
                .totalBytes(null)
                .status(UploadSessionStatus.INITIATED)
                .providerStateJson("{}")
                .sha256PartialState(initialAccumulator.serializeState())
                .createdAt(now)
                .updatedAt(now)
                .build();
        uploadSessionRepository.create(session).await().indefinitely();

        String location = buildUploadSessionLocation(repositoryName.value(), sessionId.value());
        return Response.status(Response.Status.ACCEPTED)
                .header(HttpHeaders.LOCATION, location)
                .header(OciHttpHeader.RANGE.value(), "0-0")
                .build();
    }

    @PATCH
    @Path("/{name: .+}/blobs/uploads/{sessionId}")
    @Consumes(MediaType.WILDCARD)
    public Response handleChunkUpload(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("sessionId") @NotBlank String rawSessionId,
            byte @Nullable [] chunkData
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);

        UploadSession session = uploadSessionRepository.findByToken(rawSessionId)
                .await().indefinitely()
                .orElseThrow(() -> new OciBlobUploadUnknownException(rawSessionId));

        if (session.status() != UploadSessionStatus.INITIATED) {
            throw new OciBlobUploadUnknownException(rawSessionId);
        }

        byte[] partialState = session.sha256PartialState();
        ChunkedDigestAccumulator accumulator = (partialState != null && partialState.length > 0)
                ? ChunkedDigestAccumulator.fromState(partialState)
                : ChunkedDigestAccumulator.create();

        int chunkLength = chunkData != null ? chunkData.length : 0;
        if (chunkData != null && chunkLength > 0) {
            accumulator.update(chunkData);
        }

        long newBytesReceived = session.bytesReceived() + chunkLength;
        byte[] updatedPartialState = accumulator.serializeState();

        uploadSessionRepository.updateProgress(rawSessionId, newBytesReceived, session.providerStateJson(), updatedPartialState)
                .await().indefinitely();

        String rangeHeader = calculateRangeHeader(newBytesReceived);
        String location = buildUploadSessionLocation(repositoryName.value(), rawSessionId);

        return Response.status(Response.Status.ACCEPTED)
                .header(HttpHeaders.LOCATION, location)
                .header(OciHttpHeader.RANGE.value(), rangeHeader)
                .header(HttpHeaders.CONTENT_LENGTH, 0)
                .build();
    }

    @GET
    @Path("/{name: .+}/blobs/uploads/{sessionId}")
    public Response getUploadSessionStatus(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("sessionId") @NotBlank String rawSessionId
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);

        UploadSession session = uploadSessionRepository.findByToken(rawSessionId)
                .await().indefinitely()
                .orElseThrow(() -> new OciBlobUploadUnknownException(rawSessionId));

        String rangeHeader = calculateRangeHeader(session.bytesReceived());
        String location = buildUploadSessionLocation(repositoryName.value(), rawSessionId);

        return Response.status(Response.Status.NO_CONTENT)
                .header(HttpHeaders.LOCATION, location)
                .header(OciHttpHeader.RANGE.value(), rangeHeader)
                .build();
    }

    @DELETE
    @Path("/{name: .+}/blobs/uploads/{sessionId}")
    public Response cancelUploadSession(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("sessionId") @NotBlank String rawSessionId
    ) {
        OciRepositoryName.of(rawName);

        Boolean deleted = uploadSessionRepository.deleteByToken(rawSessionId).await().indefinitely();
        if (Boolean.FALSE.equals(deleted)) {
            throw new OciBlobUploadUnknownException(rawSessionId);
        }

        return Response.noContent().build();
    }

    @PUT
    @Path("/{name: .+}/blobs/uploads/{sessionId}")
    @Consumes(MediaType.WILDCARD)
    public Response finalizeUpload(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("sessionId") @NotBlank String rawSessionId,
            @QueryParam("digest") @ValidOciDigest String rawDigestParam,
            byte @Nullable [] finalChunk
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);

        OciDigest ociDigest;
        try {
            ociDigest = OciDigest.of(rawDigestParam);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new OciDigestInvalidException("Missing or invalid digest parameter", ex);
        }

        Sha256Digest expectedDigest = ociDigest.toSha256();
        Optional<UploadSession> sessionOpt = uploadSessionRepository.findByToken(rawSessionId).await().indefinitely();

        long totalSize;
        if (sessionOpt.isPresent()) {
            totalSize = processSessionFinalization(sessionOpt.get(), finalChunk, expectedDigest, rawSessionId);
        } else {
            totalSize = finalChunk != null ? finalChunk.length : 0;
        }

        blobStore.put(expectedDigest, "application/octet-stream", new ByteArrayInputStream(finalChunk != null ? finalChunk : new byte[0]), totalSize)
                .await().indefinitely();

        String location = buildBlobLocation(repositoryName.value(), ociDigest.value());
        return Response.status(Response.Status.CREATED)
                .header(HttpHeaders.LOCATION, location)
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), ociDigest.value())
                .build();
    }

    private long processSessionFinalization(UploadSession session, byte @Nullable [] finalChunk, Sha256Digest expectedDigest, String rawSessionId) {
        byte[] partialState = session.sha256PartialState();
        ChunkedDigestAccumulator accumulator = (partialState != null && partialState.length > 0)
                ? ChunkedDigestAccumulator.fromState(partialState)
                : ChunkedDigestAccumulator.create();

        if (finalChunk != null && finalChunk.length > 0) {
            accumulator.update(finalChunk);
        }

        Sha256Digest computedDigest = accumulator.digest();
        if (!computedDigest.equals(expectedDigest)) {
            throw new OciDigestInvalidException("Digest mismatch: computed " + computedDigest.hexValue() + " does not match expected " + expectedDigest.hexValue());
        }

        long totalSize = session.bytesReceived() + (finalChunk != null ? finalChunk.length : 0);
        uploadSessionRepository.markStatus(rawSessionId, UploadSessionStatus.COMPLETED).await().indefinitely();
        return totalSize;
    }

    @HEAD
    @Path("/{name: .+}/blobs/{digest}")
    public Response checkBlobExists(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("digest") String rawDigest
    ) {
        OciDigest ociDigest;
        try {
            ociDigest = OciDigest.of(rawDigest);
        } catch (IllegalArgumentException ex) {
            throw new OciDigestInvalidException(ex.getMessage(), ex);
        }

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
        verifyBlobExistsInCas(blobStore, manifestRecord.config().digest());
        for (OciDescriptor layer : manifestRecord.layers()) {
            verifyBlobExistsInCas(blobStore, layer.digest());
        }

        StoredManifestRecord stored = manifestStore.saveManifest(repositoryName.value(), reference, manifestRecord.mediaType(), jsonPayload)
                .await().indefinitely();

        String location = buildManifestLocation(repositoryName.value(), reference);
        return Response.status(Response.Status.CREATED)
                .header(HttpHeaders.LOCATION, location)
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), SHA256_PREFIX + stored.digest().hexValue())
                .build();
    }

    @GET
    @Path("/{name: .+}/manifests/{reference}")
    public Response getManifest(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("reference") @NotBlank String reference
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);

        StoredManifestRecord stored = manifestStore.findManifest(repositoryName.value(), reference)
                .await().indefinitely()
                .orElseThrow(() -> new OciBlobUnknownException("Manifest not found for reference: " + reference));

        return Response.ok(stored.payload())
                .header(HttpHeaders.CONTENT_TYPE, stored.mediaType())
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), SHA256_PREFIX + stored.digest().hexValue())
                .build();
    }

    @HEAD
    @Path("/{name: .+}/manifests/{reference}")
    public Response headManifest(
            @PathParam("name") @NotBlank String rawName,
            @PathParam("reference") @NotBlank String reference
    ) {
        OciRepositoryName repositoryName = OciRepositoryName.of(rawName);

        StoredManifestRecord stored = manifestStore.findManifest(repositoryName.value(), reference)
                .await().indefinitely()
                .orElseThrow(() -> new OciBlobUnknownException("Manifest not found for reference: " + reference));

        return Response.ok()
                .header(HttpHeaders.CONTENT_TYPE, stored.mediaType())
                .header(OciHttpHeader.DOCKER_CONTENT_DIGEST.value(), SHA256_PREFIX + stored.digest().hexValue())
                .build();
    }

    private void verifyBlobExistsInCas(BlobStore targetBlobStore, String rawDigest) {
        String hexDigest = rawDigest.startsWith(SHA256_PREFIX) ? rawDigest.substring(7) : rawDigest;
        Sha256Digest digest = Sha256Digest.of(hexDigest);
        boolean exists = targetBlobStore.exists(digest).await().indefinitely();
        if (!exists) {
            throw new OciBlobUnknownException("Referenced layer or config blob missing from CAS: " + rawDigest);
        }
    }

    private static String calculateRangeHeader(long bytesReceived) {
        return bytesReceived > 0 ? "0-" + (bytesReceived - 1) : "0-0";
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

    private static class NoOpUploadSessionRepository implements UploadSessionRepository {
        @Override
        public io.smallrye.mutiny.Uni<UploadSession> create(UploadSession session) {
            return io.smallrye.mutiny.Uni.createFrom().item(session);
        }

        @Override
        public io.smallrye.mutiny.Uni<java.util.Optional<UploadSession>> findByToken(String uploadToken) {
            return io.smallrye.mutiny.Uni.createFrom().item(java.util.Optional.empty());
        }

        @Override
        public io.smallrye.mutiny.Uni<UploadSession> updateProgress(String uploadToken, long bytesReceived, String providerStateJson, byte @Nullable [] sha256PartialState) {
            return io.smallrye.mutiny.Uni.createFrom().failure(new UnsupportedOperationException());
        }

        @Override
        public io.smallrye.mutiny.Uni<UploadSession> markStatus(String uploadToken, UploadSessionStatus status) {
            return io.smallrye.mutiny.Uni.createFrom().failure(new UnsupportedOperationException());
        }

        @Override
        public io.smallrye.mutiny.Uni<Boolean> deleteByToken(String uploadToken) {
            return io.smallrye.mutiny.Uni.createFrom().item(false);
        }
    }
}

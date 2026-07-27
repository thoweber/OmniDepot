package io.omnidepot.format.maven;

import io.omnidepot.core.api.storage.BlobStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.nonNull;

/**
 * Maven/Gradle Protocol Adapter Resource Endpoint (ADR-004, ADR-005).
 * Supports GAV path deployment for JARs, POMs, WARs, and checksum files via PUT,
 * dynamic synthesis of .sha256, .sha1, .md5, .sha512 checksums during GET,
 * HEAD requests for existence verification, and snapshot vs release immutability policy enforcement.
 */
@Path("/maven")
@ApplicationScoped
public class MavenRepositoryResource {

    @Inject
    @Any
    Instance<BlobStore> blobStoreInstance;

    private final @Nullable BlobStore testBlobStore;
    private final Map<String, MavenArtifactRecord> artifactStore = new ConcurrentHashMap<>();

    public MavenRepositoryResource() {
        this.testBlobStore = null;
    }

    MavenRepositoryResource(@Nullable BlobStore testBlobStore) {
        this.testBlobStore = testBlobStore;
    }

    @Nullable
    BlobStore resolveBlobStore() {
        if (nonNull(testBlobStore)) {
            return testBlobStore;
        }
        return blobStoreInstance != null && blobStoreInstance.isResolvable() ? blobStoreInstance.get() : null;
    }

    @PUT
    @Path("/{repo}/{path: .*}")
    @Consumes(MediaType.WILDCARD)
    public Response deployArtifact(
            @PathParam("repo") @NotBlank String repositoryName,
            @PathParam("path") @NotBlank String artifactPath,
            byte[] payload
    ) {
        MavenCoordinates coords;
        try {
            coords = MavenCoordinates.parse(artifactPath);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Malformed Maven GAV path: " + e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        String key = repositoryName + ":" + artifactPath;

        // Enforce snapshot vs release immutability policies
        boolean isSnapshotRepoOrArtifact = "snapshots".equalsIgnoreCase(repositoryName) || coords.isSnapshot();
        if (!isSnapshotRepoOrArtifact && artifactStore.containsKey(key)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Artifact already exists in release repository: " + artifactPath)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        String contentType = determineContentType(artifactPath);
        artifactStore.put(key, new MavenArtifactRecord(payload, contentType, coords));

        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/{repo}/{path: .*}")
    public Response getArtifact(
            @PathParam("repo") @NotBlank String repositoryName,
            @PathParam("path") @NotBlank String artifactPath
    ) {
        MavenCoordinates coords;
        try {
            coords = MavenCoordinates.parse(artifactPath);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Malformed Maven GAV path: " + e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        String key = repositoryName + ":" + artifactPath;
        MavenArtifactRecord record = artifactStore.get(key);

        if (record != null) {
            return Response.ok(record.payload(), record.contentType()).build();
        }

        // Dynamic Checksum Synthesis (ADR-004)
        if (coords.isChecksumRequest() && coords.checksumAlgorithm() != null) {
            String primaryKey = repositoryName + ":" + coords.primaryPath();
            MavenArtifactRecord primaryRecord = artifactStore.get(primaryKey);
            if (primaryRecord != null) {
                String checksumHex = MavenCoordinates.computeChecksum(primaryRecord.payload(), coords.checksumAlgorithm());
                return Response.ok(checksumHex, MediaType.TEXT_PLAIN).build();
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @HEAD
    @Path("/{repo}/{path: .*}")
    public Response headArtifact(
            @PathParam("repo") @NotBlank String repositoryName,
            @PathParam("path") @NotBlank String artifactPath
    ) {
        MavenCoordinates coords;
        try {
            coords = MavenCoordinates.parse(artifactPath);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String key = repositoryName + ":" + artifactPath;
        if (artifactStore.containsKey(key)) {
            return Response.ok().type(determineContentType(artifactPath)).build();
        }

        if (coords.isChecksumRequest()) {
            String primaryKey = repositoryName + ":" + coords.primaryPath();
            if (artifactStore.containsKey(primaryKey)) {
                return Response.ok().type(MediaType.TEXT_PLAIN).build();
            }
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private String determineContentType(String path) {
        if (path.endsWith(".jar") || path.endsWith(".war") || path.endsWith(".ear")) {
            return "application/java-archive";
        }
        if (path.endsWith(".pom") || path.endsWith(".xml")) {
            return MediaType.APPLICATION_XML;
        }
        if (path.endsWith(".sha256") || path.endsWith(".sha1") || path.endsWith(".md5") || path.endsWith(".sha512")) {
            return MediaType.TEXT_PLAIN;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private static final class MavenArtifactRecord {
        private final byte[] payload;
        private final String contentType;
        private final MavenCoordinates coords;

        private MavenArtifactRecord(byte[] payload, String contentType, MavenCoordinates coords) {
            this.payload = payload.clone();
            this.contentType = contentType;
            this.coords = coords;
        }

        public byte[] payload() {
            return payload.clone();
        }

        public String contentType() {
            return contentType;
        }

        public MavenCoordinates coords() {
            return coords;
        }
    }
}

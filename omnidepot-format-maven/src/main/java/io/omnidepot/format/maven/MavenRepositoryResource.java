package io.omnidepot.format.maven;

import io.omnidepot.core.api.converter.PayloadSizeConverter;
import io.omnidepot.core.api.storage.BlobStore;
import jakarta.enterprise.context.ApplicationScoped;
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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maven/Gradle Protocol Adapter Resource Endpoint (ADR-004, ADR-005).
 * Supports GAV path deployment for JARs, POMs, WARs, and checksum files via PUT,
 * dynamic synthesis of .sha256, .sha1, .md5, .sha512 checksums during GET,
 * HEAD requests for existence verification, and snapshot vs release immutability policy enforcement.
 */
@Path("/maven")
@ApplicationScoped
@SuppressWarnings({"java:S1166", "java:S7467"})
public class MavenRepositoryResource {

    private final BlobStore blobStore;
    private final Map<String, MavenArtifactRecord> artifactStore = new ConcurrentHashMap<>();

    @Inject
    public MavenRepositoryResource(BlobStore blobStore) {
        this.blobStore = blobStore;
    }

    BlobStore blobStore() {
        return blobStore;
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
        } catch (IllegalArgumentException ignored) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Malformed Maven GAV path: " + artifactPath)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        String key = repositoryName + ":" + artifactPath;
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
        } catch (IllegalArgumentException ignored) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Malformed Maven GAV path: " + artifactPath)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        String key = repositoryName + ":" + artifactPath;
        MavenArtifactRecord storedRecord = artifactStore.get(key);
        if (storedRecord != null) {
            return Response.ok(storedRecord.payload(), storedRecord.contentType()).build();
        }

        return findChecksumResponse(repositoryName, coords);
    }

    private Response findChecksumResponse(String repositoryName, MavenCoordinates coords) {
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
        } catch (IllegalArgumentException ignored) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String key = repositoryName + ":" + artifactPath;
        String primaryKey = repositoryName + ":" + coords.primaryPath();
        boolean exists = artifactStore.containsKey(key) || (coords.isChecksumRequest() && artifactStore.containsKey(primaryKey));

        if (exists) {
            String type = artifactStore.containsKey(key) ? determineContentType(artifactPath) : MediaType.TEXT_PLAIN;
            return Response.ok().type(type).build();
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
        return (path.endsWith(".sha256") || path.endsWith(".sha1") || path.endsWith(".md5") || path.endsWith(".sha512"))
                ? MediaType.TEXT_PLAIN
                : MediaType.APPLICATION_OCTET_STREAM;
    }

    private record MavenArtifactRecord(byte[] payload, String contentType, MavenCoordinates coords) {
        public MavenArtifactRecord {
            payload = payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MavenArtifactRecord that = (MavenArtifactRecord) o;
            return Arrays.equals(payload, that.payload)
                    && Objects.equals(contentType, that.contentType)
                    && Objects.equals(coords, that.coords);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(contentType, coords);
            result = 31 * result + Arrays.hashCode(payload);
            return result;
        }

        @Override
        public String toString() {
            return "MavenArtifactRecord[payload=" + PayloadSizeConverter.formatPayload(payload)
                    + ", contentType=" + contentType
                    + ", coords=" + coords + "]";
        }
    }
}

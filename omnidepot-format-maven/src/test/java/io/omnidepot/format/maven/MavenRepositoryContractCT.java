package io.omnidepot.format.maven;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MavenRepositoryContractCT {

    private MavenRepositoryResource mavenResource;

    @BeforeEach
    void setUp() {
        mavenResource = new MavenRepositoryResource();
    }

    @Test
    @DisplayName("Given a PUT request for a release JAR, deploy artifact successfully")
    void shouldDeployReleaseJar() {
        String repo = "releases";
        String path = "io/omnidepot/sample/1.0.0/sample-1.0.0.jar";
        byte[] payload = "jar content 1.0.0".getBytes(StandardCharsets.UTF_8);

        Response putResponse = mavenResource.deployArtifact(repo, path, payload);
        assertThat(putResponse.getStatus()).isIn(Response.Status.CREATED.getStatusCode(), Response.Status.OK.getStatusCode());

        Response getResponse = mavenResource.getArtifact(repo, path);
        assertThat(getResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(getResponse.getEntity()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Given a deployed JAR, GET for .sha256, .sha1, and .md5 synthesizes correct checksums dynamically")
    void shouldSynthesizeChecksumsDynamically() {
        String repo = "releases";
        String path = "io/omnidepot/sample/1.0.0/sample-1.0.0.jar";
        byte[] payload = "test jar payload for checksums".getBytes(StandardCharsets.UTF_8);

        mavenResource.deployArtifact(repo, path, payload);

        // Calculate expected checksums
        String expectedSha256 = MavenCoordinates.computeChecksum(payload, "sha256");
        String expectedSha1 = MavenCoordinates.computeChecksum(payload, "sha1");
        String expectedMd5 = MavenCoordinates.computeChecksum(payload, "md5");

        Response sha256Resp = mavenResource.getArtifact(repo, path + ".sha256");
        assertThat(sha256Resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(sha256Resp.getEntity()).isEqualTo(expectedSha256);

        Response sha1Resp = mavenResource.getArtifact(repo, path + ".sha1");
        assertThat(sha1Resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(sha1Resp.getEntity()).isEqualTo(expectedSha1);

        Response md5Resp = mavenResource.getArtifact(repo, path + ".md5");
        assertThat(md5Resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(md5Resp.getEntity()).isEqualTo(expectedMd5);
    }

    @Test
    @DisplayName("Given a release repository, deploying an existing artifact path again returns 409 Conflict")
    void shouldEnforceReleaseImmutabilityPolicy() {
        String repo = "releases";
        String path = "io/omnidepot/sample/1.0.0/sample-1.0.0.jar";
        byte[] payload1 = "jar content v1".getBytes(StandardCharsets.UTF_8);
        byte[] payload2 = "jar content v2".getBytes(StandardCharsets.UTF_8);

        Response firstPut = mavenResource.deployArtifact(repo, path, payload1);
        assertThat(firstPut.getStatus()).isIn(Response.Status.CREATED.getStatusCode(), Response.Status.OK.getStatusCode());

        Response secondPut = mavenResource.deployArtifact(repo, path, payload2);
        assertThat(secondPut.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    @DisplayName("Given a snapshot repository, deploying an existing artifact path again overwrites payload successfully")
    void shouldAllowSnapshotOverwriting() {
        String repo = "snapshots";
        String path = "io/omnidepot/sample/1.0.0-SNAPSHOT/sample-1.0.0-SNAPSHOT.jar";
        byte[] payload1 = "snapshot payload build 1".getBytes(StandardCharsets.UTF_8);
        byte[] payload2 = "snapshot payload build 2".getBytes(StandardCharsets.UTF_8);

        Response firstPut = mavenResource.deployArtifact(repo, path, payload1);
        assertThat(firstPut.getStatus()).isIn(Response.Status.CREATED.getStatusCode(), Response.Status.OK.getStatusCode());

        Response secondPut = mavenResource.deployArtifact(repo, path, payload2);
        assertThat(secondPut.getStatus()).isIn(Response.Status.CREATED.getStatusCode(), Response.Status.OK.getStatusCode());

        Response getResponse = mavenResource.getArtifact(repo, path);
        assertThat(getResponse.getEntity()).isEqualTo(payload2);
    }

    @Test
    @DisplayName("Given a HEAD request for an existing artifact, return 200 OK without body")
    void shouldHandleHeadRequest() {
        String repo = "releases";
        String path = "io/omnidepot/sample/2.0.0/sample-2.0.0.pom";
        byte[] payload = "<project></project>".getBytes(StandardCharsets.UTF_8);

        mavenResource.deployArtifact(repo, path, payload);

        Response headExist = mavenResource.headArtifact(repo, path);
        assertThat(headExist.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        Response headChecksumExist = mavenResource.headArtifact(repo, path + ".sha256");
        assertThat(headChecksumExist.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        Response headNotExist = mavenResource.headArtifact(repo, "non/existent/path/1.0/path-1.0.jar");
        assertThat(headNotExist.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @DisplayName("Given a malformed GAV path, return 400 Bad Request")
    void shouldReturnBadRequestOnMalformedPath() {
        Response putResp = mavenResource.deployArtifact("releases", "invalid-path", "data".getBytes());
        assertThat(putResp.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        Response getResp = mavenResource.getArtifact("releases", "invalid-path");
        assertThat(getResp.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }
}

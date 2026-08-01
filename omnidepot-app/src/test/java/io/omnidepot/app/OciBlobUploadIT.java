package io.omnidepot.app;

import io.omnidepot.format.oci.OciHttpHeader;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static io.omnidepot.format.oci.OciHttpHeader.DOCKER_CONTENT_DIGEST;
import static io.omnidepot.format.oci.OciHttpHeader.DOCKER_DISTRIBUTION_API_VERSION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class OciBlobUploadIT {

    @Test
    @DisplayName("Given OCI client ping - when GET /v2/ is called - then 200 OK with registry/2.0 header is returned")
    void shouldReturnOciApiVersionHeader() {
        given()
                .when()
                .get("/v2/")
                .then()
                .statusCode(200)
                .header(DOCKER_DISTRIBUTION_API_VERSION.value(), equalTo("registry/2.0"));
    }

    @Test
    @DisplayName("Given valid repository - when initiating blob upload POST /v2/{name}/blobs/uploads - then 202 Accepted with session location is returned")
    void shouldInitiateOciBlobUploadSession() {
        given()
                .when()
                .post("/v2/library/ubuntu/blobs/uploads")
                .then()
                .statusCode(202)
                .header(HttpHeaders.LOCATION, containsString("/v2/library/ubuntu/blobs/uploads/"))
                .header(OciHttpHeader.RANGE.value(), equalTo("0-0"));
    }

    @Test
    @DisplayName("Given valid mount parameters - when POST /v2/{name}/blobs/uploads?mount=sha256:...&from=... - then 201 Created is returned")
    void shouldPerformOciCrossRepoMount() {
        String digest = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        given()
                .queryParam("mount", digest)
                .queryParam("from", "library/alpine")
                .when()
                .post("/v2/library/my-app/blobs/uploads")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/v2/library/my-app/blobs/" + digest))
                .header(DOCKER_CONTENT_DIGEST.value(), equalTo(digest));
    }

    @Test
    @DisplayName("Given active session - when sending chunked PATCH requests and finalizing PUT - then 201 Created with digest is returned")
    void shouldPerformResumableChunkedBlobUploadSession() throws Exception {
        String location = given()
                .when()
                .post("/v2/library/debian/blobs/uploads")
                .then()
                .statusCode(202)
                .extract()
                .header(HttpHeaders.LOCATION);

        byte[] chunk1 = "Layer chunk 1 content. ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "Layer chunk 2 content.".getBytes(StandardCharsets.UTF_8);

        given()
                .body(chunk1)
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .when()
                .patch(location)
                .then()
                .statusCode(202)
                .header(OciHttpHeader.RANGE.value(), equalTo("0-" + (chunk1.length - 1)));

        given()
                .body(chunk2)
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .when()
                .patch(location)
                .then()
                .statusCode(202)
                .header(OciHttpHeader.RANGE.value(), equalTo("0-" + (chunk1.length + chunk2.length - 1)));

        byte[] fullContent = "Layer chunk 1 content. Layer chunk 2 content.".getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String expectedDigest = "sha256:" + HexFormat.of().formatHex(md.digest(fullContent));

        given()
                .queryParam("digest", expectedDigest)
                .when()
                .put(location)
                .then()
                .statusCode(201)
                .header(DOCKER_CONTENT_DIGEST.value(), equalTo(expectedDigest));
    }

    @Test
    @DisplayName("Given active session and digest parameter - when finalizing upload via PUT - then 201 Created is returned")
    void shouldFinalizeMonolithicOciBlobUpload() {
        String digest = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        given()
                .queryParam("digest", digest)
                .when()
                .put("/v2/library/ubuntu/blobs/uploads/session-789")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/v2/library/ubuntu/blobs/" + digest))
                .header(DOCKER_CONTENT_DIGEST.value(), equalTo(digest));
    }

    @Test
    @DisplayName("Given invalid digest format - when finalizing upload via PUT - then 400 Bad Request with OCI error JSON is returned")
    void shouldReturnOciCompliantErrorJsonForInvalidDigest() {
        given()
                .queryParam("digest", "invalid-hash-value")
                .when()
                .put("/v2/library/ubuntu/blobs/uploads/session-789")
                .then()
                .statusCode(400)
                .contentType("application/json")
                .body("errors[0].code", equalTo("DIGEST_INVALID"))
                .body("errors[0].message", containsString("invalid-hash-value"));
    }
}

package io.omnidepot.app;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

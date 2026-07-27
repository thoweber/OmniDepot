package io.omnidepot.app;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.omnidepot.format.oci.OciHttpHeader.DOCKER_CONTENT_DIGEST;
import static io.omnidepot.format.oci.OciMediaType.OCI_IMAGE_MANIFEST;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration test verifying OCI Manifest REST endpoints (PUT, GET, HEAD) against omnidepot-app (Sub-Issue #9).
 */
@QuarkusTest
class OciNativeDockerIT {

    /** SHA-256 digest of the raw {@link #VALID_MANIFEST_JSON} body bytes as sent over the wire. */
    private static final String MANIFEST_DIGEST =
            "sha256:3276b6439e7be19a67369fe36104eb89b1ec4733892c002f2f0ddd5774d1cbd0";

    private static final String VALID_MANIFEST_JSON = """
            {
              "schemaVersion": 2,
              "mediaType": "application/vnd.oci.image.manifest.v1+json",
              "config": {
                "mediaType": "application/vnd.oci.image.config.v1+json",
                "size": 7023,
                "digest": "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
              },
              "layers": []
            }
            """;

    @Test
    @DisplayName("Given valid OCI manifest payload - when PUT /v2/{name}/manifests/{reference} - then 201 Created is returned")
    void shouldUploadAndIngestOciManifest() {
        given()
                .contentType(OCI_IMAGE_MANIFEST.value())
                .body(VALID_MANIFEST_JSON)
                .when()
                .put("/v2/my-org/alpine/manifests/1.0.0")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, equalTo("/v2/my-org/alpine/manifests/1.0.0"))
                .header(DOCKER_CONTENT_DIGEST.value(), equalTo(MANIFEST_DIGEST));
    }

    @Test
    @DisplayName("Given ingested OCI manifest - when GET /v2/{name}/manifests/{reference} - then 200 OK with payload is returned")
    void shouldGetIngestedOciManifest() {
        given()
                .contentType(OCI_IMAGE_MANIFEST.value())
                .body(VALID_MANIFEST_JSON)
                .when()
                .put("/v2/my-org/alpine/manifests/latest");

        given()
                .when()
                .get("/v2/my-org/alpine/manifests/latest")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, equalTo(OCI_IMAGE_MANIFEST.value()))
                .header(DOCKER_CONTENT_DIGEST.value(), equalTo(MANIFEST_DIGEST))
                .body(equalTo(VALID_MANIFEST_JSON));
    }

    @Test
    @DisplayName("Given ingested OCI manifest - when HEAD /v2/{name}/manifests/{reference} - then 200 OK with headers is returned")
    void shouldHeadIngestedOciManifest() {
        given()
                .contentType(OCI_IMAGE_MANIFEST.value())
                .body(VALID_MANIFEST_JSON)
                .when()
                .put("/v2/my-org/alpine/manifests/v2.0.0");

        given()
                .when()
                .head("/v2/my-org/alpine/manifests/v2.0.0")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, equalTo(OCI_IMAGE_MANIFEST.value()))
                .header(DOCKER_CONTENT_DIGEST.value(), equalTo(MANIFEST_DIGEST));
    }
}

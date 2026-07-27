package io.omnidepot.app;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * Integration test verifying OCI Manifest REST endpoints (PUT, GET, HEAD) against omnidepot-app (Sub-Issue #9).
 */
@QuarkusTest
class OciNativeDockerIT {

    private static final String CONFIG_DIGEST = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

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
                .contentType("application/vnd.oci.image.manifest.v1+json")
                .body(VALID_MANIFEST_JSON)
                .when()
                .put("/v2/my-org/alpine/manifests/1.0.0")
                .then()
                .statusCode(201)
                .header("Location", equalTo("/v2/my-org/alpine/manifests/1.0.0"))
                .header("Docker-Content-Digest", startsWith("sha256:"));
    }

    @Test
    @DisplayName("Given ingested OCI manifest - when GET /v2/{name}/manifests/{reference} - then 200 OK with payload is returned")
    void shouldGetIngestedOciManifest() {
        given()
                .contentType("application/vnd.oci.image.manifest.v1+json")
                .body(VALID_MANIFEST_JSON)
                .when()
                .put("/v2/my-org/alpine/manifests/latest");

        given()
                .when()
                .get("/v2/my-org/alpine/manifests/latest")
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/vnd.oci.image.manifest.v1+json"))
                .header("Docker-Content-Digest", startsWith("sha256:"))
                .body(equalTo(VALID_MANIFEST_JSON));
    }

    @Test
    @DisplayName("Given ingested OCI manifest - when HEAD /v2/{name}/manifests/{reference} - then 200 OK with headers is returned")
    void shouldHeadIngestedOciManifest() {
        given()
                .contentType("application/vnd.oci.image.manifest.v1+json")
                .body(VALID_MANIFEST_JSON)
                .when()
                .put("/v2/my-org/alpine/manifests/v2.0.0");

        given()
                .when()
                .head("/v2/my-org/alpine/manifests/v2.0.0")
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/vnd.oci.image.manifest.v1+json"))
                .header("Docker-Content-Digest", startsWith("sha256:"));
    }
}

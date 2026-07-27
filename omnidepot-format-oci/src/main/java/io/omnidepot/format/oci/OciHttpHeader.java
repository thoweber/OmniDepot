package io.omnidepot.format.oci;

/**
 * Enumeration of OCI V2 Distribution Specification and Docker Registry V2 protocol-specific HTTP header names.
 *
 * <p>Standard HTTP headers (e.g. {@code Content-Type}, {@code Location}) MUST be referenced via
 * {@link jakarta.ws.rs.core.HttpHeaders} constants. OCI/Docker-specific headers that are not part
 * of the Jakarta EE standard MUST be referenced via this enum.
 *
 * @see <a href="https://github.com/opencontainers/distribution-spec/blob/main/spec.md">OCI Distribution Spec</a>
 * @see jakarta.ws.rs.core.HttpHeaders
 */
public enum OciHttpHeader {

    /** {@code Docker-Content-Digest} — the canonical OCI digest of a blob or manifest response body. */
    DOCKER_CONTENT_DIGEST("Docker-Content-Digest"),

    /** {@code Docker-Distribution-API-Version} — declares OCI V2 registry compliance. */
    DOCKER_DISTRIBUTION_API_VERSION("Docker-Distribution-API-Version"),

    /** {@code Docker-Upload-UUID} — identifies an active chunked upload session. */
    DOCKER_UPLOAD_UUID("Docker-Upload-UUID"),

    /** {@code OCI-Chunk-Min-Length} — minimum chunk size for chunked blob uploads (OCI Distribution 1.1+). */
    OCI_CHUNK_MIN_LENGTH("OCI-Chunk-Min-Length"),

    /**
     * {@code Range} — used in OCI blob upload acceptance responses to indicate the current byte range
     * received. Not a constant in {@link jakarta.ws.rs.core.HttpHeaders}.
     */
    RANGE("Range");

    private final String value;

    OciHttpHeader(String value) {
        this.value = value;
    }

    /**
     * Returns the HTTP header name string (e.g. {@code "Docker-Content-Digest"}).
     *
     * @return HTTP header name
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}

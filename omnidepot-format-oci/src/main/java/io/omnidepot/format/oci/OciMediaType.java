package io.omnidepot.format.oci;

/**
 * Enumeration of OCI V2 Distribution Specification and Docker Registry V2 media types.
 *
 * <p>All OCI/Docker protocol media-type constants MUST be referenced via this enum rather
 * than raw string literals throughout the {@code omnidepot-format-oci} module and its tests.
 *
 * @see <a href="https://github.com/opencontainers/image-spec/blob/main/media-types.md">OCI Media Types</a>
 */
public enum OciMediaType {

    /** OCI Image Manifest V1 — {@code application/vnd.oci.image.manifest.v1+json}. */
    OCI_IMAGE_MANIFEST("application/vnd.oci.image.manifest.v1+json"),

    /** OCI Image Index (multi-arch) — {@code application/vnd.oci.image.index.v1+json}. */
    OCI_IMAGE_INDEX("application/vnd.oci.image.index.v1+json"),

    /** OCI Image Config — {@code application/vnd.oci.image.config.v1+json}. */
    OCI_IMAGE_CONFIG("application/vnd.oci.image.config.v1+json"),

    /** OCI Image Layer (tar+gzip) — {@code application/vnd.oci.image.layer.v1.tar+gzip}. */
    OCI_IMAGE_LAYER_TAR_GZIP("application/vnd.oci.image.layer.v1.tar+gzip"),

    /** Docker Schema 2 Manifest — {@code application/vnd.docker.distribution.manifest.v2+json}. */
    DOCKER_MANIFEST_V2("application/vnd.docker.distribution.manifest.v2+json"),

    /** Docker Schema 2 Manifest List (multi-arch) — {@code application/vnd.docker.distribution.manifest.list.v2+json}. */
    DOCKER_MANIFEST_LIST_V2("application/vnd.docker.distribution.manifest.list.v2+json"),

    /** Docker Image Config — {@code application/vnd.docker.container.image.v1+json}. */
    DOCKER_IMAGE_CONFIG("application/vnd.docker.container.image.v1+json"),

    /** Docker Layer (tar+gzip) — {@code application/vnd.docker.image.rootfs.diff.tar.gzip}. */
    DOCKER_LAYER_TAR_GZIP("application/vnd.docker.image.rootfs.diff.tar.gzip");

    private final String value;

    OciMediaType(String value) {
        this.value = value;
    }

    /**
     * Returns the IANA media-type string value (e.g. {@code application/vnd.oci.image.manifest.v1+json}).
     *
     * @return IANA media-type string
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}

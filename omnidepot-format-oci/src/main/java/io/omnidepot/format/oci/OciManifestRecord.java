package io.omnidepot.format.oci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.omnidepot.core.api.storage.Sha256Digest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Immutable record representing an OCI Image Manifest V2 Schema 2 or Docker Schema 2 JSON payload.
 * Provides canonical SHA-256 digest computation and Jackson JSON parsing.
 */
@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
record OciManifestRecord(
        @JsonProperty("schemaVersion") @Min(2) Integer schemaVersion,
        @JsonProperty("mediaType") @NotBlank String mediaType,
        @JsonProperty("config") @NotNull OciDescriptor config,
        @JsonProperty("layers") @NotNull List<OciDescriptor> layers
) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parses an OCI Manifest JSON string into an immutable {@link OciManifestRecord}.
     *
     * @param jsonString raw JSON string
     * @return deserialized OciManifestRecord instance
     * @throws OciProtocolException if parsing fails
     */
    public static OciManifestRecord fromJson(String jsonString) {
        if (jsonString.isBlank()) {
            throw new OciManifestInvalidException("Manifest JSON payload cannot be null or blank");
        }
        try {
            return OBJECT_MAPPER.readValue(jsonString, OciManifestRecord.class);
        } catch (IOException ex) {
            throw new OciManifestInvalidException("Failed to parse OCI Image Manifest JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * Calculates the canonical SHA-256 digest for raw manifest bytes.
     *
     * @param manifestBytes raw payload bytes
     * @return strongly-typed {@link OciDigest}
     */
    public static OciDigest calculateDigest(byte[] manifestBytes) {
        if (manifestBytes.length == 0) {
            throw new OciManifestInvalidException("Cannot compute digest for empty manifest bytes");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(manifestBytes);
            String hexHash = HexFormat.of().formatHex(hashBytes);
            return OciDigest.fromSha256(Sha256Digest.of(hexHash));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 MessageDigest algorithm not available", ex);
        }
    }
}

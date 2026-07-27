package io.omnidepot.format.oci;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;

/**
 * Immutable descriptor for OCI/Docker image configuration or layer blob references.
 */
@NullMarked
record OciDescriptor(
        @JsonProperty("mediaType") @NotBlank String mediaType,
        @JsonProperty("size") @Min(0) Long size,
        @JsonProperty("digest") @NotBlank String digest
) {
}

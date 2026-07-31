package io.omnidepot.format.oci;

import io.omnidepot.core.api.storage.Sha256Digest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OciDigestTest {

    @Test
    @DisplayName("Given valid Sha256Digest - when generating OciDigest - then sha256: prefix is appended with correct capacity")
    void shouldFormatOciDigestFromSha256() {
        // Given
        String hex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        Sha256Digest coreDigest = Sha256Digest.of(hex);

        // When
        OciDigest ociDigest = OciDigest.fromSha256(coreDigest);

        // Then
        assertThat(ociDigest.value()).isEqualTo("sha256:" + hex);
        assertThat(ociDigest.toSha256()).isEqualTo(coreDigest);
    }

    @Test
    @DisplayName("Given valid raw string - when creating OciDigest via of() - then value and toSha256 are populated correctly")
    void shouldParseValidOciDigest() {
        // Given
        String raw = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        // When
        OciDigest ociDigest = OciDigest.of(raw);

        // Then
        assertThat(ociDigest.value()).isEqualTo(raw);
        assertThat(ociDigest.toSha256().hexValue()).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("Given raw string missing sha256: prefix - when creating OciDigest - then IllegalArgumentException is thrown")
    void shouldRejectOciDigestWithoutAlgorithmPrefix() {
        assertThatThrownBy(() -> OciDigest.of("invalid-prefix-value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given valid prefix but invalid hex encoding - when creating OciDigest - then IllegalArgumentException is thrown")
    void shouldRejectOciDigestWithInvalidHexEncoding() {
        assertThatThrownBy(() -> OciDigest.of("sha256:not-a-valid-hex-string"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

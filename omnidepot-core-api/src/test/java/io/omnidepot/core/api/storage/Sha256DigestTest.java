package io.omnidepot.core.api.storage;

import io.omnidepot.core.api.test.support.DigestObjectMother;
import io.omnidepot.core.api.test.support.DigestTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256DigestTest {

    @Test
    @DisplayName("Given a valid 64-char hex string - when creating Sha256Digest - then hex value is preserved")
    void shouldParseValidHexDigest() {
        // Given
        String validHex = DigestObjectMother.SAMPLE_SHA256_HEX;

        // When
        Sha256Digest digest = Sha256Digest.of(validHex);

        // Then
        DigestTestSupport.assertValidDigestValue(digest, validHex);
        assertThat(digest.hexValue()).isEqualTo(validHex);
    }

    @Test
    @DisplayName("Given a sha256: prefixed string - when creating Sha256Digest - then prefix is stripped and value is normalized")
    void shouldStripSha256Prefix() {
        // Given
        Sha256Digest digest = DigestObjectMother.prefixedDigest();

        // Then
        DigestTestSupport.assertValidDigestValue(digest, DigestObjectMother.SAMPLE_SHA256_HEX);
    }

    @Test
    @DisplayName("Given an uppercase hex string - when creating Sha256Digest - then hex value is normalized to lowercase")
    void shouldNormalizeUppercaseHex() {
        // Given
        Sha256Digest digest = DigestObjectMother.uppercaseDigest();

        // Then
        DigestTestSupport.assertValidDigestValue(digest, DigestObjectMother.SAMPLE_SHA256_HEX);
    }

    @Test
    @DisplayName("Given invalid or non-hex string inputs - when creating Sha256Digest - then IllegalArgumentException is thrown")
    void shouldRejectInvalidDigestFormats() {
        DigestTestSupport.assertInvalidDigestCreationFails("too-short");
        DigestTestSupport.assertInvalidDigestCreationFails("zzzz" + DigestObjectMother.SAMPLE_SHA256_HEX.substring(4));
        DigestTestSupport.assertInvalidDigestCreationFails("");
    }
}

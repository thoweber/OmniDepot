package io.omnidepot.core.api.test.support;

import io.omnidepot.core.api.storage.Sha256Digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Support class containing reusable AssertJ assertions and verification helpers for Sha256Digest tests.
 */
public class DigestTestSupport {

    public static void assertValidDigestValue(Sha256Digest digest, String expectedHex) {
        assertThat(digest).as("Digest must not be null").isNotNull();
        assertThat(digest.hexValue()).as("Hex value should match normalized expected string").isEqualTo(expectedHex.toLowerCase());
    }

    public static void assertInvalidDigestCreationFails(String invalidInput) {
        assertThatThrownBy(() -> Sha256Digest.of(invalidInput))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

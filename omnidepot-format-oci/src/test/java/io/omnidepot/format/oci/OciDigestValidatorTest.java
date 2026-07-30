package io.omnidepot.format.oci;

import io.omnidepot.format.oci.validation.OciDigestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class OciDigestValidatorTest {

    private OciDigestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OciDigestValidator();
    }

    @Test
    @DisplayName("Given valid SHA-256 digest string - when isValid is called - then returns true")
    void shouldAcceptValidSha256Digest() {
        String validDigest = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        boolean result = validator.isValid(validDigest, null);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "invalid-digest",
            "sha256:12345",
            "sha256:ZZZZc44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    })
    @DisplayName("Given invalid digest strings - when isValid is called - then returns false")
    void shouldRejectInvalidDigestStrings(String invalidDigest) {
        boolean result = validator.isValid(invalidDigest, null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given null string - when isValid is called - then returns false")
    void shouldRejectNullString() {
        boolean result = validator.isValid(null, null);

        assertThat(result).isFalse();
    }
}

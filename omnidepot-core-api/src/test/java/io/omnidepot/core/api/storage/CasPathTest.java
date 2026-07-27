package io.omnidepot.core.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CasPathTest {

    @Test
    @DisplayName("Given Sha256Digest - when generating CasPath - then formatting and capacities match single source of truth")
    void shouldFormatCasPathFromSha256() {
        // Given
        String hex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        Sha256Digest digest = Sha256Digest.of(hex);

        // When
        CasPath casPath = CasPath.fromSha256(digest);

        // Then
        assertThat(casPath.value()).isEqualTo("blobs/sha256/e3/b0/" + hex);
    }

    @Test
    @DisplayName("Given blank string - when creating CasPath - then IllegalArgumentException is thrown")
    void shouldRejectBlankCasPath() {
        assertThatThrownBy(() -> CasPath.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

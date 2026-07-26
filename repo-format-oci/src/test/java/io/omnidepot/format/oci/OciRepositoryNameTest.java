package io.omnidepot.format.oci;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OciRepositoryNameTest {

    @Test
    @DisplayName("Given valid raw repository string - when creating OciRepositoryName - then value is normalized to lowercase")
    void shouldNormalizeValidRepositoryName() {
        // When
        OciRepositoryName name = OciRepositoryName.of("  Library/Ubuntu  ");

        // Then
        assertThat(name.value()).isEqualTo("library/ubuntu");
    }

    @Test
    @DisplayName("Given invalid repository string - when creating OciRepositoryName - then OciNameInvalidException is thrown")
    void shouldRejectInvalidRepositoryName() {
        assertThatThrownBy(() -> OciRepositoryName.of("INVALID_NAME!!"))
                .isInstanceOf(OciNameInvalidException.class);
    }
}

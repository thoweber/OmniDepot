package io.omnidepot.core.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadSessionIdTest {

    @Test
    @DisplayName("Given valid string - when creating UploadSessionId - then value is trimmed and preserved")
    void shouldCreateValidUploadSessionId() {
        // When
        UploadSessionId id = UploadSessionId.of("  session-123  ");

        // Then
        assertThat(id.value()).isEqualTo("session-123");
    }

    @Test
    @DisplayName("Given blank string - when creating UploadSessionId - then IllegalArgumentException is thrown")
    void shouldRejectBlankUploadSessionId() {
        assertThatThrownBy(() -> UploadSessionId.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package io.omnidepot.core.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageExceptionTest {

    @Test
    @DisplayName("Given message and cause - when creating StorageException - then message and cause are preserved")
    void shouldConstructStorageException() {
        // Given
        var cause = new RuntimeException("Underlying IO error");

        // When
        var ex1 = new StorageException("Storage error");
        var ex2 = new StorageException("Storage error with cause", cause);

        // Then
        assertThat(ex1.getMessage()).isEqualTo("Storage error");
        assertThat(ex1.getCause()).isNull();

        assertThat(ex2.getMessage()).isEqualTo("Storage error with cause");
        assertThat(ex2.getCause()).isSameAs(cause);
    }
}

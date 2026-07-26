package io.omnidepot.core.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlobReadExceptionTest {

    @Test
    @DisplayName("Given message and cause - when creating BlobReadException - then properties match")
    void shouldConstructBlobReadException() {
        var cause = new RuntimeException("Read error");
        var ex = new BlobReadException("Failed read", cause);

        assertThat(ex.getMessage()).isEqualTo("Failed read");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}

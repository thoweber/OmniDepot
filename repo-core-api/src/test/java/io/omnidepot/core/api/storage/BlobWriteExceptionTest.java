package io.omnidepot.core.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlobWriteExceptionTest {

    @Test
    @DisplayName("Given message and cause - when creating BlobWriteException - then properties match")
    void shouldConstructBlobWriteException() {
        var cause = new RuntimeException("Disk full");
        var ex = new BlobWriteException("Failed write", cause);

        assertThat(ex.getMessage()).isEqualTo("Failed write");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}

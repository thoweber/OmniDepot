package io.omnidepot.core.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlobDeletionExceptionTest {

    @Test
    @DisplayName("Given message and cause - when creating BlobDeletionException - then properties match")
    void shouldConstructBlobDeletionException() {
        var cause = new RuntimeException("Permission denied");
        var ex = new BlobDeletionException("Failed delete", cause);

        assertThat(ex.getMessage()).isEqualTo("Failed delete");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}

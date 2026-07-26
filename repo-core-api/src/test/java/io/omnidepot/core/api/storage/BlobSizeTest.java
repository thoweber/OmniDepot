package io.omnidepot.core.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlobSizeTest {

    @Test
    @DisplayName("Given zero bytes - when creating BlobSize - then static ZERO instance is returned")
    void shouldReturnStaticZeroInstanceForZeroBytes() {
        // When
        BlobSize b1 = BlobSize.ZERO;
        BlobSize b2 = BlobSize.of(0L);

        // Then
        assertThat(b1).isSameAs(BlobSize.ZERO);
        assertThat(b2).isSameAs(BlobSize.ZERO);
    }

    @Test
    @DisplayName("Given negative bytes - when creating BlobSize - then IllegalArgumentException is thrown")
    void shouldRejectNegativeBlobSize() {
        assertThatThrownBy(() -> BlobSize.of(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package io.omnidepot.core.api.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageValueObjectsTest {

    @Test
    @DisplayName("Given Sha256Digest - when generating OciDigest and CasPath - then formatting and capacities match single source of truth")
    void shouldFormatOciDigestAndCasPathFromSha256() {
        // Given
        String hex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        Sha256Digest digest = Sha256Digest.of(hex);

        // When
        OciDigest ociDigest = OciDigest.fromSha256(digest);
        CasPath casPath = CasPath.fromSha256(digest);

        // Then
        assertThat(ociDigest.value()).isEqualTo("sha256:" + hex);
        assertThat(casPath.value()).isEqualTo("blobs/sha256/e3/b0/" + hex);
    }

    @Test
    @DisplayName("Given zero bytes - when creating BlobSize - then static ZERO instance is returned")
    void shouldReturnStaticZeroInstanceForZeroBytes() {
        // When
        BlobSize b1 = BlobSize.zero();
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

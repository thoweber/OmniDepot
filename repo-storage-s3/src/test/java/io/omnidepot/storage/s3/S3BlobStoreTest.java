package io.omnidepot.storage.s3;

import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.omnidepot.core.api.test.support.DigestObjectMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3BlobStoreTest {

    private S3BlobStore s3BlobStore;

    @BeforeEach
    void setUp() {
        s3BlobStore = new S3BlobStore("test-bucket");
    }

    @Test
    @DisplayName("Given a blob payload - when put() is invoked - then S3 BlobDescriptor is returned")
    void shouldReturnS3BlobDescriptorOnPut() {
        // Given
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();
        InputStream data = new ByteArrayInputStream(new byte[0]);

        // When
        BlobDescriptor descriptor = s3BlobStore.put(digest, "application/octet-stream", data, 0L).await().indefinitely();

        // Then
        assertThat(descriptor.digest()).isEqualTo(digest);
        assertThat(descriptor.storagePath()).isEqualTo("s3://test-bucket/blobs/sha256/e3/b0/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("Given a digest - when openStream() is called - UnsupportedOperationException is thrown until S3 client is wired")
    void shouldThrowUnsupportedOperationExceptionOnOpenStream() {
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();

        assertThatThrownBy(() -> s3BlobStore.openStream(digest).await().indefinitely())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Given a digest - when exists(), getDescriptor(), and delete() are invoked - then expected stubs are returned")
    void shouldReturnExpectedStubsForS3Operations() {
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();

        assertThat(s3BlobStore.exists(digest).await().indefinitely()).isFalse();
        assertThat(s3BlobStore.getDescriptor(digest).await().indefinitely()).isEmpty();
        assertThat(s3BlobStore.delete(digest).await().indefinitely()).isTrue();
    }
}

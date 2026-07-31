package io.omnidepot.storage.fs;

import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.BlobReadException;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.omnidepot.core.api.test.support.DigestObjectMother;
import io.omnidepot.storage.fs.test.support.FileSystemTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSystemBlobStoreTest {

    @TempDir
    Path tempStorageDirectory;

    private FileSystemBlobStore blobStore;

    @BeforeEach
    void setUp() {
        blobStore = new FileSystemBlobStore(tempStorageDirectory.toString());
    }

    @Test
    @DisplayName("Given a blob payload, put() stores content on disk and returns descriptor")
    void shouldStoreBlobOnDisk() {
        // Given
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();
        InputStream payloadStream = FileSystemTestSupport.createSampleStream();
        long size = FileSystemTestSupport.getSampleStreamLength();

        // When
        BlobDescriptor descriptor = blobStore.put(digest, "text/plain", payloadStream, size).await().indefinitely();

        // Then
        FileSystemTestSupport.assertBlobDescriptorMatches(descriptor, "text/plain", size);
        assertThat(blobStore.exists(digest).await().indefinitely()).as("Blob should exist after put()").isTrue();
    }

    @Test
    @DisplayName("Given a stored blob, openStream() retrieves identical byte content")
    void shouldReadStoredBlobStream() throws Exception {
        // Given
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();
        blobStore.put(digest, "text/plain", FileSystemTestSupport.createSampleStream(), FileSystemTestSupport.getSampleStreamLength()).await().indefinitely();

        // When
        InputStream readStream = blobStore.openStream(digest).await().indefinitely();

        // Then
        FileSystemTestSupport.assertStreamContentEquals(readStream, FileSystemTestSupport.SAMPLE_PAYLOAD);
    }

    @Test
    @DisplayName("Given a stored blob, getDescriptor() returns populated Optional with metadata")
    void shouldReturnPopulatedDescriptorForExistingBlob() {
        // Given
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();
        blobStore.put(digest, "text/plain", FileSystemTestSupport.createSampleStream(), FileSystemTestSupport.getSampleStreamLength()).await().indefinitely();

        // When
        Optional<BlobDescriptor> descriptorOpt = blobStore.getDescriptor(digest).await().indefinitely();

        // Then
        assertThat(descriptorOpt).isPresent();
        assertThat(descriptorOpt.get().digest()).isEqualTo(digest);
        assertThat(descriptorOpt.get().mediaType()).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("Given non-existent blob, exists() returns false and descriptor is empty")
    void shouldHandleNonExistentBlob() {
        // Given
        Sha256Digest missingDigest = DigestObjectMother.alternateDigest();

        // When
        Boolean exists = blobStore.exists(missingDigest).await().indefinitely();
        Optional<BlobDescriptor> descriptor = blobStore.getDescriptor(missingDigest).await().indefinitely();

        // Then
        assertThat(exists).isFalse();
        assertThat(descriptor).isEmpty();
    }

    @Test
    @DisplayName("Given non-existent blob, openStream() throws BlobReadException")
    void shouldThrowBlobReadExceptionForMissingBlobStream() {
        // Given
        Sha256Digest missingDigest = DigestObjectMother.alternateDigest();

        // When / Then
        var awaiter = blobStore.openStream(missingDigest).await();
        assertThatThrownBy(awaiter::indefinitely)
                .isInstanceOf(BlobReadException.class)
                .hasMessageContaining("Failed to open stream for blob");
    }

    @Test
    @DisplayName("Given an existing blob, delete() removes it from storage")
    void shouldDeleteBlobFromStorage() {
        // Given
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();
        blobStore.put(digest, "text/plain", FileSystemTestSupport.createSampleStream(), FileSystemTestSupport.getSampleStreamLength()).await().indefinitely();

        // When
        Boolean deleted = blobStore.delete(digest).await().indefinitely();

        // Then
        assertThat(deleted).isTrue();
        assertThat(blobStore.exists(digest).await().indefinitely()).isFalse();
    }
}

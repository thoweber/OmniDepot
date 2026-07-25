package io.omnidepot.core.api.storage;

import io.omnidepot.core.api.test.support.BlobObjectMother;
import io.omnidepot.core.api.test.support.DigestObjectMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlobDescriptorTest {

    @Test
    @DisplayName("Given valid parameters, BlobDescriptor constructs record successfully")
    void shouldConstructValidBlobDescriptor() {
        // Given
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();

        // When
        BlobDescriptor descriptor = BlobObjectMother.createBlobWithDigestAndSize(digest, 2048L);

        // Then
        assertThat(descriptor.id()).isNotNull();
        assertThat(descriptor.digest()).isEqualTo(digest);
        assertThat(descriptor.sizeBytes()).isEqualTo(2048L);
    }

    @Test
    @DisplayName("Given negative size, constructor throws IllegalArgumentException")
    void shouldRejectNegativeSizeBytes() {
        Sha256Digest digest = DigestObjectMother.emptyPayloadDigest();

        assertThatThrownBy(() ->
                new BlobDescriptor(UUID.randomUUID().toString(), digest, -1L, "text/plain", "/path", Instant.now())
        ).isInstanceOf(IllegalArgumentException.class);
    }
}

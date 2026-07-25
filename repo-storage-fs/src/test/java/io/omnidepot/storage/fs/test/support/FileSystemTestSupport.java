package io.omnidepot.storage.fs.test.support;

import io.omnidepot.core.api.storage.BlobDescriptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Support class encapsulating stream operations, payload generation, and AssertJ assertions for FileSystem tests.
 */
public class FileSystemTestSupport {

    public static final String SAMPLE_PAYLOAD = "OmniDepot Filesystem Storage Unit Test Payload";

    public static InputStream createSampleStream() {
        return new ByteArrayInputStream(SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8));
    }

    public static long getSampleStreamLength() {
        return SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8).length;
    }

    public static void assertStreamContentEquals(InputStream stream, String expectedText) throws Exception {
        assertThat(stream).as("Input stream must not be null").isNotNull();
        byte[] bytes = stream.readAllBytes();
        String actualText = new String(bytes, StandardCharsets.UTF_8);
        assertThat(actualText).as("Stream payload bytes must match expected content").isEqualTo(expectedText);
    }

    public static void assertBlobDescriptorMatches(BlobDescriptor descriptor, String expectedMediaType, long expectedSize) {
        assertThat(descriptor).as("Descriptor must not be null").isNotNull();
        assertThat(descriptor.mediaType()).as("Media type should match").isEqualTo(expectedMediaType);
        assertThat(descriptor.sizeBytes()).as("Size bytes should match").isEqualTo(expectedSize);
    }
}

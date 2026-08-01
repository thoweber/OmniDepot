package io.omnidepot.core.api.upload;

import io.omnidepot.core.api.storage.Sha256Digest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkedDigestAccumulatorTest {

    @Test
    @DisplayName("Given empty input - when digested - then produces standard empty SHA-256 hash")
    void shouldProduceCorrectDigestForEmptyInput() {
        ChunkedDigestAccumulator accumulator = ChunkedDigestAccumulator.create();
        Sha256Digest result = accumulator.digest();
        String expectedHex = calculateExpectedHex(new byte[0]);

        assertThat(result.hexValue()).isEqualTo(expectedHex)
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("Given single chunk - when accumulated - then calculates correct Sha256Digest")
    void shouldAccumulateSingleChunkAndProduceCorrectDigest() {
        byte[] chunk = "Hello OmniDepot!".getBytes(StandardCharsets.UTF_8);

        ChunkedDigestAccumulator accumulator = ChunkedDigestAccumulator.create();
        accumulator.update(chunk);

        Sha256Digest result = accumulator.digest();
        String expectedHex = calculateExpectedHex(chunk);

        assertThat(result.hexValue()).isEqualTo(expectedHex);
    }

    @Test
    @DisplayName("Given multiple chunks - when accumulated sequentially - then matches full content digest")
    void shouldAccumulateMultipleChunksIncrementally() {
        byte[] chunk1 = "Part 1 of layer content. ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "Part 2 of layer content.".getBytes(StandardCharsets.UTF_8);
        byte[] fullContent = "Part 1 of layer content. Part 2 of layer content.".getBytes(StandardCharsets.UTF_8);

        ChunkedDigestAccumulator accumulator = ChunkedDigestAccumulator.create();
        accumulator.update(chunk1);
        accumulator.update(chunk2);

        Sha256Digest result = accumulator.digest();
        String expectedHex = calculateExpectedHex(fullContent);

        assertThat(result.hexValue()).isEqualTo(expectedHex);
    }

    @Test
    @DisplayName("Given payload sizes - when computed via accumulator - then strictly matches MessageDigest output")
    void shouldMatchMessageDigestAcrossVaryingPayloadSizes() {
        int[] sizes = {0, 1, 55, 56, 57, 63, 64, 65, 127, 128, 129, 1024, 1048576};
        for (int size : sizes) {
            byte[] data = new byte[size];
            for (int i = 0; i < size; i++) {
                data[i] = (byte) (i % 256);
            }

            ChunkedDigestAccumulator accumulator = ChunkedDigestAccumulator.create();
            accumulator.update(data);

            Sha256Digest result = accumulator.digest();
            String expectedHex = calculateExpectedHex(data);

            assertThat(result.hexValue()).isEqualTo(expectedHex);
        }
    }

    @Test
    @DisplayName("Given partial state - when serialized and restored across chunks - then maintains digest state")
    void shouldSerializeAndRestorePartialState() {
        byte[] chunk1 = "Chunk Alpha | ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "Chunk Beta | ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk3 = "Chunk Gamma".getBytes(StandardCharsets.UTF_8);

        byte[] fullContent = "Chunk Alpha | Chunk Beta | Chunk Gamma".getBytes(StandardCharsets.UTF_8);

        // Session 1: Process Chunk 1
        ChunkedDigestAccumulator acc1 = ChunkedDigestAccumulator.create();
        acc1.update(chunk1);
        byte[] serializedState1 = acc1.serializeState();

        assertThat(serializedState1).hasSize(108);

        // Session 2: Restore from state 1, process Chunk 2
        ChunkedDigestAccumulator acc2 = ChunkedDigestAccumulator.fromState(serializedState1);
        acc2.update(chunk2);
        byte[] serializedState2 = acc2.serializeState();

        // Session 3: Restore from state 2, process Chunk 3 and finalize
        ChunkedDigestAccumulator acc3 = ChunkedDigestAccumulator.fromState(serializedState2);
        acc3.update(chunk3);

        Sha256Digest finalDigest = acc3.digest();
        String expectedHex = calculateExpectedHex(fullContent);

        assertThat(finalDigest.hexValue()).isEqualTo(expectedHex);
    }

    @Test
    @DisplayName("Given invalid state byte arrays - when restored - then throws IllegalArgumentException")
    void shouldValidateFromStateInputs() {
        assertThatThrownBy(() -> ChunkedDigestAccumulator.fromState(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stateBytes must not be null");

        assertThatThrownBy(() -> ChunkedDigestAccumulator.fromState(new byte[107]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("state length must be exactly 108 bytes");

        assertThatThrownBy(() -> ChunkedDigestAccumulator.fromState(new byte[109]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("state length must be exactly 108 bytes");
    }

    @Test
    @DisplayName("Given state payload with out-of-bound headers - when restored - then throws IllegalArgumentException")
    void shouldRejectCorruptedStateHeaderBounds() {
        // Create valid 108-byte array
        byte[] stateBytes = ChunkedDigestAccumulator.create().serializeState();

        // Corrupt bufOfs to negative (-1)
        byte[] corruptBufOfsNeg = Arrays.copyOf(stateBytes, 108);
        ByteBuffer.wrap(corruptBufOfsNeg, 40, 4).putInt(-1);
        assertThatThrownBy(() -> ChunkedDigestAccumulator.fromState(corruptBufOfsNeg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("corrupted header bounds");

        // Corrupt bufOfs to >= 64 (64)
        byte[] corruptBufOfsHigh = Arrays.copyOf(stateBytes, 108);
        ByteBuffer.wrap(corruptBufOfsHigh, 40, 4).putInt(64);
        assertThatThrownBy(() -> ChunkedDigestAccumulator.fromState(corruptBufOfsHigh))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("corrupted header bounds");

        // Corrupt count to negative (-1)
        byte[] corruptCountNeg = Arrays.copyOf(stateBytes, 108);
        ByteBuffer.wrap(corruptCountNeg, 32, 8).putLong(-1L);
        assertThatThrownBy(() -> ChunkedDigestAccumulator.fromState(corruptCountNeg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("corrupted header bounds");
    }

    @Test
    @DisplayName("Given invalid update parameters - when updating accumulator - then throws expected exceptions")
    void shouldValidateUpdateParameters() {
        ChunkedDigestAccumulator acc = ChunkedDigestAccumulator.create();
        byte[] chunk = new byte[10];

        assertThatThrownBy(() -> acc.update(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> acc.update(null, 0, 5))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> acc.update(chunk, -1, 5))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> acc.update(chunk, 0, -1))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> acc.update(chunk, 5, 6))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    private static String calculateExpectedHex(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

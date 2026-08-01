package io.omnidepot.core.api.upload;

import io.omnidepot.core.api.storage.Sha256Digest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkedDigestAccumulatorTest {

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

        assertThat(serializedState1).isNotNull().isNotEmpty();

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
    @DisplayName("Given corrupted byte array - when restoring state - then throws IllegalArgumentException")
    void shouldFailOnCorruptedState() {
        byte[] invalidState = new byte[]{0x01, 0x02, 0x03, 0x04};

        assertThatThrownBy(() -> ChunkedDigestAccumulator.fromState(invalidState))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid digest state");
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
